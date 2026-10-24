package com.bookleaf.support.dto;

import com.bookleaf.support.dto.AdminDtos.*;
import com.bookleaf.support.dto.AuthorDtos.*;
import com.bookleaf.support.entity.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity -> DTO mapping in one place. Callers must pass entities whose lazy associations are already
 * loaded (repository entity graphs) or that are used inside a transaction.
 */
public final class DtoMapper {
    private DtoMapper() {}

    public static BookResponse toBook(Book b) {
        return new BookResponse(b.getId(), b.getTitle(), b.getIsbn(), b.getGenre(), b.getPublicationDate(), b.getStatus(),
                b.getMrp(), b.getTotalCopiesSold(), b.getTotalRoyaltyEarned(), b.getRoyaltyPaid(), b.getRoyaltyPending());
    }

    public static AuthorTicketView toAuthorView(Ticket t, List<TicketResponse> responses) {
        Book book = t.getBook();
        return new AuthorTicketView(t.getId(), book == null ? null : book.getId(), book == null ? null : book.getTitle(),
                t.getSubject(), t.getDescription(), t.getStatus().name(), t.getStatus().label(), t.getAttachmentUrl(),
                attachmentUrls(t), attachmentViews(t, "/author"),
                t.getCreatedAt(), t.getLastActivityAt(),
                responses.stream().map(r -> new TicketMessage(r.getId(), r.getSender().getName(),
                        r.getSenderRole().name(), r.getMessage(), r.getCreatedAt())).toList());
    }

    public static TicketSummary toSummary(Ticket t) {
        Book book = t.getBook();
        User admin = t.getAssignedAdmin();
        long ageHours = t.getCreatedAt() == null ? 0 : Duration.between(t.getCreatedAt(), LocalDateTime.now()).toHours();
        return new TicketSummary(t.getId(), t.getAuthor().getId(), t.getAuthor().getName(), t.getAuthor().getEmail(),
                book == null ? null : book.getId(), book == null ? null : book.getTitle(),
                t.getSubject(), t.getCategory(), t.getCategory().label(), t.getPriority(),
                t.getStatus(), t.getStatus().label(),
                admin == null ? null : admin.getId(), admin == null ? null : admin.getName(),
                t.isAiClassified(), t.isCategoryOverridden(), t.isPriorityOverridden(),
                t.getCreatedAt(), t.getLastActivityAt(), ageHours);
    }

    public static AdminTicketDetail toDetail(Ticket t, List<TicketResponse> responses, List<InternalNote> notes) {
        return new AdminTicketDetail(toSummary(t), t.getDescription(), t.getAttachmentUrl(), attachmentUrls(t),
                attachmentViews(t, "/admin"), t.getAiReasoning(),
                t.getAiDraft() != null, t.getBook() == null ? null : toBook(t.getBook()),
                responses.stream().map(r -> new AdminMessage(r.getId(), r.getSender().getName(), r.getSenderRole().name(),
                        r.getMessage(), r.isAiDrafted(), r.getCreatedAt())).toList(),
                notes.stream().map(n -> new NoteView(n.getId(), n.getAdmin().getName(), n.getNote(), n.getCreatedAt())).toList());
    }

    private static List<String> attachmentUrls(Ticket ticket) {
        if (ticket.getAttachmentUrls() != null && !ticket.getAttachmentUrls().isEmpty()) {
            return ticket.getAttachmentUrls();
        }
        return ticket.getAttachmentUrl() == null ? List.of() : List.of(ticket.getAttachmentUrl());
    }

    private static List<AttachmentView> attachmentViews(Ticket ticket, String rolePath) {
        if (ticket.getAttachments() == null) return List.of();
        return ticket.getAttachments().stream()
                .map(attachment -> new AttachmentView(attachment.getId(), attachment.getFileName(),
                        rolePath + "/tickets/" + ticket.getId() + "/attachments/" + attachment.getId()))
                .toList();
    }
}
