package com.bookleaf.support.service;

import com.bookleaf.support.dto.AuthorDtos.*;
import com.bookleaf.support.dto.DtoMapper;
import com.bookleaf.support.entity.*;
import com.bookleaf.support.exception.ResourceNotFoundException;
import com.bookleaf.support.repository.BookRepository;
import com.bookleaf.support.repository.TicketAttachmentRepository;
import com.bookleaf.support.repository.TicketRepository;
import com.bookleaf.support.repository.TicketResponseRepository;
import com.bookleaf.support.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Every method takes the authenticated author's id and scopes all data access to it. */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthorService {

    private final BookRepository books;
    private final TicketRepository tickets;
    private final TicketAttachmentRepository attachments;
    private final AttachmentStorageService attachmentStorage;
    private final TicketResponseRepository responses;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<BookResponse> listBooks(Long authorId) {
        return books.findByAuthorIdOrderByPublicationDateDescTitleAsc(authorId).stream().map(DtoMapper::toBook).toList();
    }

    public AuthorTicketView createTicket(Long authorId, CreateTicketRequest req) {
        return createTicket(authorId, req, List.of());
    }

    public AuthorTicketView createTicket(Long authorId, CreateTicketRequest req, List<MultipartFile> files) {
        List<MultipartFile> uploads = files == null ? List.of() : files;
        if (uploads.size() > 5) {
            throw new IllegalArgumentException("You can attach up to 5 files.");
        }
        uploads.forEach(attachmentStorage::validate);
        Book book = null;
        if (req.bookId() != null) {
            book = books.findByIdAndAuthorId(req.bookId(), authorId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Book", req.bookId()));
        }
        // Saved with neutral defaults first; AI triage then refines category/priority asynchronously.
        List<String> attachmentUrls = new ArrayList<>(new LinkedHashSet<>(
                req.attachmentUrls() == null ? List.of() : req.attachmentUrls()));
        if (req.attachmentUrl() != null && !req.attachmentUrl().isBlank()) {
            attachmentUrls.add(0, req.attachmentUrl().trim());
            attachmentUrls = new ArrayList<>(new LinkedHashSet<>(attachmentUrls));
        }
        Ticket ticket = tickets.save(Ticket.builder()
                .author(users.getReferenceById(authorId))
                .book(book)
                .subject(req.subject().trim())
                .description(req.description().trim())
                .attachmentUrl(attachmentUrls.isEmpty() ? null : attachmentUrls.get(0))
                .attachmentUrls(attachmentUrls)
                .build());
        List<String> storedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : uploads) {
                String key = attachmentStorage.store(file);
                storedKeys.add(key);
                TicketAttachment attachment = new TicketAttachment();
                attachment.setTicket(ticket);
                attachment.setFileName(safeFileName(file.getOriginalFilename()));
                attachment.setStorageKey(key);
                attachment.setContentType(file.getContentType());
                attachment.setFileSize(file.getSize());
                ticket.getAttachments().add(attachments.save(attachment));
            }
        } catch (IOException | RuntimeException e) {
            for (String key : storedKeys) {
                try {
                    attachmentStorage.delete(key);
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Unable to save attachments. Please try again.", e);
        }
        events.publishEvent(new TicketCreatedEvent(ticket.getId()));
        return DtoMapper.toAuthorView(ticket, List.of());
    }

    @Transactional(readOnly = true)
    public DownloadableAttachment getAttachment(Long authorId, Long ticketId, Long attachmentId) {
        ownTicket(authorId, ticketId);
        TicketAttachment attachment = attachments.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> ResourceNotFoundException.of("Attachment", attachmentId));
        return new DownloadableAttachment(attachmentStorage.resolve(attachment.getStorageKey()),
                attachment.getFileName(), attachment.getContentType());
    }

    /** {@code since} (a lastActivityAt value from a previous response) returns only tickets that changed: cheap polling. */
    @Transactional(readOnly = true)
    public List<AuthorTicketView> listTickets(Long authorId, LocalDateTime since) {
        List<Ticket> mine = tickets.findForAuthor(authorId, since);
        if (mine.isEmpty()) return List.of();
        Map<Long, List<TicketResponse>> byTicket = responses
                .findByTicketIdInOrderByCreatedAtAsc(mine.stream().map(Ticket::getId).toList()).stream()
                .collect(Collectors.groupingBy(r -> r.getTicket().getId()));
        return mine.stream().map(t -> DtoMapper.toAuthorView(t, byTicket.getOrDefault(t.getId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public AuthorTicketView getTicket(Long authorId, Long ticketId) {
        Ticket t = ownTicket(authorId, ticketId);
        return DtoMapper.toAuthorView(t, responses.findByTicketIdOrderByCreatedAtAsc(ticketId));
    }

    /** Author follow-up on an existing ticket. Reopens resolved tickets, invalidates the cached AI draft. */
    public AuthorTicketView reply(Long authorId, Long ticketId, ReplyRequest req) {
        Ticket t = ownTicket(authorId, ticketId);
        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException("This ticket is closed. Please submit a new query.");
        }
        responses.save(TicketResponse.builder().ticket(t).sender(t.getAuthor()).senderRole(Role.AUTHOR)
                .message(req.message().trim()).build());
        if (t.getStatus() == TicketStatus.RESOLVED) t.setStatus(TicketStatus.OPEN);
        t.setAiDraft(null);
        t.touch();
        return DtoMapper.toAuthorView(t, responses.findByTicketIdOrderByCreatedAtAsc(ticketId));
    }

    private Ticket ownTicket(Long authorId, Long ticketId) {
        return tickets.fetchByIdAndAuthorId(ticketId, authorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Ticket", ticketId));
    }

    private static String safeFileName(String fileName) {
        String name = Path.of((fileName == null ? "attachment" : fileName).replace('\\', '/'))
                .getFileName().toString().replaceAll("[\\p{Cntrl}]", "");
        if (name.isBlank()) name = "attachment";
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }

    public record DownloadableAttachment(Path path, String fileName, String contentType) {}
}
