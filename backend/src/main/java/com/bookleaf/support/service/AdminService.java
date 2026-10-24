//package com.bookleaf.support.service;
//
//import com.bookleaf.support.dto.AdminDtos.*;
//import com.bookleaf.support.dto.DtoMapper;
//import com.bookleaf.support.entity.*;
//import com.bookleaf.support.exception.AiUnavailableException;
//import com.bookleaf.support.exception.ResourceNotFoundException;
//import com.bookleaf.support.repository.InternalNoteRepository;
//import com.bookleaf.support.repository.TicketRepository;
//import com.bookleaf.support.repository.TicketResponseRepository;
//import com.bookleaf.support.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class AdminService {
//
//    private static final int THREAD_MESSAGES_IN_PROMPT = 3;
//    private static final int THREAD_MESSAGE_MAX_CHARS = 400;
//
//    private final TicketRepository tickets;
//    private final TicketResponseRepository responses;
//    private final InternalNoteRepository notes;
//    private final UserRepository users;
//    private final AiService ai;
//    private final TicketTriageService triage;
//
//    // ------------------------------------------------------------------ queue & detail
//
//    @Transactional(readOnly = true)
//    public PageResponse<TicketSummary> search(TicketStatus status, TicketCategory category, Priority priority,
//                                              Long assignedAdminId, LocalDate from, LocalDate to, String q,
//                                              int page, int size) {
//        LocalDateTime fromTs = from == null ? null : from.atStartOfDay();
//        LocalDateTime toTs = to == null ? null : to.plusDays(1).atStartOfDay(); // "to" is inclusive
//        String query = (q == null || q.isBlank()) ? null : q.trim();
//        Page<Ticket> result = tickets.search(status, category, priority, assignedAdminId, fromTs, toTs, query,
//                PageRequest.of(page, size)); // ordering lives in the query: urgent + oldest first
//        return PageResponse.of(result.map(DtoMapper::toSummary));
//    }
//
//    @Transactional(readOnly = true)
//    public AdminTicketDetail getTicket(Long id) {
//        return detail(find(id));
//    }
//
//    @Transactional(readOnly = true)
//    public List<AdminUser> listAdmins() {
//        return users.findAllByRoleOrderByNameAsc(Role.ADMIN).stream()
//                .map(user -> new AdminUser(user.getId(), user.getName(), user.getEmail()))
//                .toList();
//    }
//
//    // ------------------------------------------------------------------ management
//
//    public AdminTicketDetail updateStatus(Long id, TicketStatus status) {
//        Ticket t = find(id);
//        t.setStatus(status);
//        t.touch(); // surfaces the status change to the author's poll
//        return detail(t);
//    }
//
//    /** Human override of the AI category; later AI re-triage will not change it. */
//    public AdminTicketDetail overrideCategory(Long id, TicketCategory category) {
//        Ticket t = find(id);
//        t.overrideCategory(category);
//        t.setAiDraft(null); // the draft was written against the old category's policy
//        return detail(t);
//    }
//
//    public AdminTicketDetail overridePriority(Long id, Priority priority) {
//        Ticket t = find(id);
//        t.overridePriority(priority);
//        return detail(t);
//    }
//
//    public AdminTicketDetail assignToMe(Long id, Long adminId) {
//        Ticket t = find(id);
//        t.setAssignedAdmin(users.getReferenceById(adminId));
//        if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
//        t.touch();
//        return detail(t);
//    }
//
//    public AdminTicketDetail assign(Long id, Long adminId) {
//        Ticket t = find(id);
//        if (adminId == null) {
//            t.setAssignedAdmin(null);
//        } else {
//            User admin = users.findById(adminId)
//                    .filter(user -> user.getRole() == Role.ADMIN)
//                    .orElseThrow(() -> ResourceNotFoundException.of("Admin", adminId));
//            t.setAssignedAdmin(admin);
//            if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
//        }
//        t.touch();
//        return detail(t);
//    }
//
//    public AdminTicketDetail addNote(Long id, Long adminId, AddNoteRequest req) {
//        Ticket t = find(id);
//        notes.save(InternalNote.builder().ticket(t).admin(users.getReferenceById(adminId)).note(req.note().trim()).build());
//        return detail(t); // no touch(): internal notes are invisible to the author
//    }
//
//    public AdminTicketDetail reply(Long id, Long adminId, AdminReplyRequest req) {
//        Ticket t = find(id);
//        if (t.getStatus() == TicketStatus.CLOSED && req.newStatus() == null) {
//            throw new IllegalStateException("Ticket is closed. Reopen it (set a new status) before replying.");
//        }
//        User admin = users.getReferenceById(adminId);
//        responses.save(TicketResponse.builder().ticket(t).sender(admin).senderRole(Role.ADMIN)
//                .message(req.message().trim()).aiDrafted(req.aiDrafted()).build());
//        if (t.getAssignedAdmin() == null) t.setAssignedAdmin(admin);
//        if (req.newStatus() != null) t.setStatus(req.newStatus());
//        else if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
//        t.touch();
//        return detail(t);
//    }
//
//    // ------------------------------------------------------------------ AI
//
//    /**
//     * Returns a cached draft if present, otherwise asks the LLM. NOT_SUPPORTED so no DB transaction/connection
//     * is held open during the network call. AI failure is a normal, 200-level outcome (available=false).
//     */
//    @Transactional(propagation = Propagation.NOT_SUPPORTED)
//    public AiDraftResponse generateDraft(Long id, boolean regenerate) {
//        Ticket t = find(id);
//        if (!regenerate && t.getAiDraft() != null) {
//            return new AiDraftResponse(true, t.getAiDraft(), null, true);
//        }
//        List<TicketResponse> thread = responses.findByTicketIdOrderByCreatedAtAsc(id);
//        List<String> recent = thread.subList(Math.max(0, thread.size() - THREAD_MESSAGES_IN_PROMPT), thread.size()).stream()
//                .map(r -> r.getSenderRole() + ": " + clip(r.getMessage()))
//                .toList();
//        AiService.DraftContext ctx = new AiService.DraftContext(t.getAuthor().getName(), t.getSubject(), t.getDescription(),
//                t.getCategory(), bookSummary(t.getBook()), recent);
//        try {
//            String draft = ai.draft(ctx);
//            tickets.saveAiDraft(id, draft);
//            return new AiDraftResponse(true, draft, null, false);
//        } catch (AiUnavailableException e) {
//            return new AiDraftResponse(false, null,
//                    "AI drafting is temporarily unavailable. You can write the response manually.", false);
//        }
//    }
//
//    /** Re-runs triage, e.g. for tickets that fell back to rule-based triage during an outage. Respects overrides. */
//    @Transactional(propagation = Propagation.NOT_SUPPORTED)
//    public AdminTicketDetail retriage(Long id) {
//        triage.triage(id);
//        return detail(find(id)); // re-read: the merged entity returned by save() has uninitialised lazy associations
//    }
//
//    // ------------------------------------------------------------------ helpers
//
//    private Ticket find(Long id) {
//        return tickets.fetchById(id).orElseThrow(() -> ResourceNotFoundException.of("Ticket", id));
//    }
//
//    private AdminTicketDetail detail(Ticket t) {
//        return DtoMapper.toDetail(t, responses.findByTicketIdOrderByCreatedAtAsc(t.getId()),
//                notes.findByTicketIdOrderByCreatedAtAsc(t.getId()));
//    }
//
//    /** Compact, factual one-liner for the prompt; production-stage books are flagged explicitly. */
//    private static String bookSummary(Book b) {
//        if (b == null) return null;
//        return "Title: " + b.getTitle()
//                + "; ISBN: " + (b.getIsbn() == null ? "not yet assigned" : b.getIsbn())
//                + "; Genre: " + nz(b.getGenre())
//                + "; Status: " + nz(b.getStatus())
//                + "; Publication date: " + (b.getPublicationDate() == null ? "not published yet" : b.getPublicationDate())
//                + "; MRP: Rs " + nz(b.getMrp())
//                + "; Copies sold: " + nz(b.getTotalCopiesSold())
//                + "; Royalty earned: Rs " + nz(b.getTotalRoyaltyEarned())
//                + "; Royalty paid: Rs " + nz(b.getRoyaltyPaid())
//                + "; Royalty pending: Rs " + nz(b.getRoyaltyPending());
//    }
//
//    private static String nz(Object o) { return o == null ? "n/a" : o.toString(); }
//
//    private static String clip(String s) {
//        return s.length() <= THREAD_MESSAGE_MAX_CHARS ? s : s.substring(0, THREAD_MESSAGE_MAX_CHARS) + "...";
//    }
//}



//package com.bookleaf.support.service;
//
//import com.bookleaf.support.dto.AdminDtos.*;
//import com.bookleaf.support.dto.DtoMapper;
//import com.bookleaf.support.entity.*;
//import com.bookleaf.support.exception.AiUnavailableException;
//import com.bookleaf.support.exception.ResourceNotFoundException;
//import com.bookleaf.support.repository.InternalNoteRepository;
//import com.bookleaf.support.repository.TicketRepository;
//import com.bookleaf.support.repository.TicketResponseRepository;
//import com.bookleaf.support.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class AdminService {
//
//    private static final int THREAD_MESSAGES_IN_PROMPT = 3;
//    private static final int THREAD_MESSAGE_MAX_CHARS = 400;
//
//    private final TicketRepository tickets;
//    private final TicketResponseRepository responses;
//    private final InternalNoteRepository notes;
//    private final UserRepository users;
//    private final AiService ai;
//    private final TicketTriageService triage;
//    private final ApplicationEventPublisher events;
//
//    // ------------------------------------------------------------------ queue & detail
//
//    @Transactional(readOnly = true)
//    public PageResponse<TicketSummary> search(TicketStatus status, TicketCategory category, Priority priority,
//                                              Long assignedAdminId, LocalDate from, LocalDate to, String q,
//                                              int page, int size) {
//        LocalDateTime fromTs = from == null ? null : from.atStartOfDay();
//        LocalDateTime toTs = to == null ? null : to.plusDays(1).atStartOfDay(); // "to" is inclusive
//        String query = (q == null || q.isBlank()) ? null : q.trim();
//        Page<Ticket> result = tickets.search(status, category, priority, assignedAdminId, fromTs, toTs, query,
//                PageRequest.of(page, size)); // ordering lives in the query: urgent + oldest first
//        return PageResponse.of(result.map(DtoMapper::toSummary));
//    }
//
//    @Transactional(readOnly = true)
//    public AdminTicketDetail getTicket(Long id) {
//        return detail(find(id));
//    }
//
//    @Transactional(readOnly = true)
//    public List<AdminUser> listAdmins() {
//        return users.findAllByRoleOrderByNameAsc(Role.ADMIN).stream()
//                .map(user -> new AdminUser(user.getId(), user.getName(), user.getEmail()))
//                .toList();
//    }
//
//    // ------------------------------------------------------------------ management
//
//    public AdminTicketDetail updateStatus(Long id, TicketStatus status) {
//        Ticket t = find(id);
//        t.setStatus(status);
//        t.touch(); // surfaces the status change to the author's poll
//        notifyAuthor(t);
//        return detail(t);
//    }
//
//    /** Human override of the AI category; later AI re-triage will not change it. */
//    public AdminTicketDetail overrideCategory(Long id, TicketCategory category) {
//        Ticket t = find(id);
//        t.overrideCategory(category);
//        t.setAiDraft(null); // the draft was written against the old category's policy
//        return detail(t);
//    }
//
//    public AdminTicketDetail overridePriority(Long id, Priority priority) {
//        Ticket t = find(id);
//        t.overridePriority(priority);
//        return detail(t);
//    }
//
//    public AdminTicketDetail assignToMe(Long id, Long adminId) {
//        Ticket t = find(id);
//        t.setAssignedAdmin(users.getReferenceById(adminId));
//        if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
//        t.touch();
//        notifyAuthor(t);
//        return detail(t);
//    }
//
//    public AdminTicketDetail assign(Long id, Long adminId) {
//        Ticket t = find(id);
//        if (adminId == null) {
//            t.setAssignedAdmin(null);
//        } else {
//            User admin = users.findById(adminId)
//                    .filter(user -> user.getRole() == Role.ADMIN)
//                    .orElseThrow(() -> ResourceNotFoundException.of("Admin", adminId));
//            t.setAssignedAdmin(admin);
//            if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
//        }
//        t.touch();
//        notifyAuthor(t);
//        return detail(t);
//    }
//
//    public AdminTicketDetail addNote(Long id, Long adminId, AddNoteRequest req) {
//        Ticket t = find(id);
//        notes.save(InternalNote.builder().ticket(t).admin(users.getReferenceById(adminId)).note(req.note().trim()).build());
//        return detail(t); // no touch(): internal notes are invisible to the author
//    }
//
//    public AdminTicketDetail reply(Long id, Long adminId, AdminReplyRequest req) {
//        Ticket t = find(id);
//        if (t.getStatus() == TicketStatus.CLOSED && req.newStatus() == null) {
//            throw new IllegalStateException("Ticket is closed. Reopen it (set a new status) before replying.");
//        }
//        User admin = users.getReferenceById(adminId);
//        responses.save(TicketResponse.builder().ticket(t).sender(admin).senderRole(Role.ADMIN)
//                .message(req.message().trim()).aiDrafted(req.aiDrafted()).build());
//        if (t.getAssignedAdmin() == null) t.setAssignedAdmin(admin);
//        if (req.newStatus() != null) t.setStatus(req.newStatus());
//        else if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
//        t.touch();
//        notifyAuthor(t);
//        return detail(t);
//    }
//
//    // ------------------------------------------------------------------ AI
//
//    /**
//     * Returns a cached draft if present, otherwise asks the LLM. NOT_SUPPORTED so no DB transaction/connection
//     * is held open during the network call. AI failure is a normal, 200-level outcome (available=false).
//     */
//    @Transactional(propagation = Propagation.NOT_SUPPORTED)
//    public AiDraftResponse generateDraft(Long id, boolean regenerate) {
//        Ticket t = find(id);
//        if (!regenerate && t.getAiDraft() != null) {
//            return new AiDraftResponse(true, t.getAiDraft(), null, true);
//        }
//        List<TicketResponse> thread = responses.findByTicketIdOrderByCreatedAtAsc(id);
//        List<String> recent = thread.subList(Math.max(0, thread.size() - THREAD_MESSAGES_IN_PROMPT), thread.size()).stream()
//                .map(r -> r.getSenderRole() + ": " + clip(r.getMessage()))
//                .toList();
//        AiService.DraftContext ctx = new AiService.DraftContext(t.getAuthor().getName(), t.getSubject(), t.getDescription(),
//                t.getCategory(), bookSummary(t.getBook()), recent);
//        try {
//            String draft = ai.draft(ctx);
//            tickets.saveAiDraft(id, draft);
//            return new AiDraftResponse(true, draft, null, false);
//        } catch (AiUnavailableException e) {
//            return new AiDraftResponse(false, null,
//                    "AI drafting is temporarily unavailable. You can write the response manually.", false);
//        }
//    }
//
//    /** Re-runs triage, e.g. for tickets that fell back to rule-based triage during an outage. Respects overrides. */
//    @Transactional(propagation = Propagation.NOT_SUPPORTED)
//    public AdminTicketDetail retriage(Long id) {
//        triage.triage(id);
//        return detail(find(id)); // re-read: the merged entity returned by save() has uninitialised lazy associations
//    }
//
//    // ------------------------------------------------------------------ helpers
//
//    /** Pushed to the author's open SSE connections after this transaction commits. */
//    private void notifyAuthor(Ticket t) {
//        events.publishEvent(new TicketUpdatedEvent(t.getAuthor().getId(), t.getId()));
//    }
//
//    private Ticket find(Long id) {
//        return tickets.fetchById(id).orElseThrow(() -> ResourceNotFoundException.of("Ticket", id));
//    }
//
//    private AdminTicketDetail detail(Ticket t) {
//        return DtoMapper.toDetail(t, responses.findByTicketIdOrderByCreatedAtAsc(t.getId()),
//                notes.findByTicketIdOrderByCreatedAtAsc(t.getId()));
//    }
//
//    /** Compact, factual one-liner for the prompt; production-stage books are flagged explicitly. */
//    private static String bookSummary(Book b) {
//        if (b == null) return null;
//        return "Title: " + b.getTitle()
//                + "; ISBN: " + (b.getIsbn() == null ? "not yet assigned" : b.getIsbn())
//                + "; Genre: " + nz(b.getGenre())
//                + "; Status: " + nz(b.getStatus())
//                + "; Publication date: " + (b.getPublicationDate() == null ? "not published yet" : b.getPublicationDate())
//                + "; MRP: Rs " + nz(b.getMrp())
//                + "; Copies sold: " + nz(b.getTotalCopiesSold())
//                + "; Royalty earned: Rs " + nz(b.getTotalRoyaltyEarned())
//                + "; Royalty paid: Rs " + nz(b.getRoyaltyPaid())
//                + "; Royalty pending: Rs " + nz(b.getRoyaltyPending());
//    }
//
//    private static String nz(Object o) { return o == null ? "n/a" : o.toString(); }
//
//    private static String clip(String s) {
//        return s.length() <= THREAD_MESSAGE_MAX_CHARS ? s : s.substring(0, THREAD_MESSAGE_MAX_CHARS) + "...";
//    }
//}


package com.bookleaf.support.service;

import com.bookleaf.support.dto.AdminDtos.*;
import com.bookleaf.support.dto.DtoMapper;
import com.bookleaf.support.entity.*;
import com.bookleaf.support.exception.AiUnavailableException;
import com.bookleaf.support.exception.ResourceNotFoundException;
import com.bookleaf.support.repository.InternalNoteRepository;
import com.bookleaf.support.repository.TicketRepository;
import com.bookleaf.support.repository.TicketAttachmentRepository;
import com.bookleaf.support.repository.TicketResponseRepository;
import com.bookleaf.support.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private static final int THREAD_MESSAGES_IN_PROMPT = 3;
    private static final int THREAD_MESSAGE_MAX_CHARS = 400;

    private final TicketRepository tickets;
    private final TicketAttachmentRepository attachments;
    private final AttachmentStorageService attachmentStorage;
    private final TicketResponseRepository responses;
    private final InternalNoteRepository notes;
    private final UserRepository users;
    private final AiService ai;
    private final TicketTriageService triage;
    private final ApplicationEventPublisher events;

    // ------------------------------------------------------------------ queue & detail

    @Transactional(readOnly = true)
    public PageResponse<TicketSummary> search(TicketStatus status, TicketCategory category, Priority priority,
                                              Long assignedAdminId, LocalDate from, LocalDate to, String q,
                                              int page, int size) {
        LocalDateTime fromTs = from == null ? null : from.atStartOfDay();
        LocalDateTime toTs = to == null ? null : to.plusDays(1).atStartOfDay(); // "to" is inclusive
        String query = (q == null || q.isBlank()) ? null : q.trim();
        Page<Ticket> result = tickets.search(status, category, priority, assignedAdminId, fromTs, toTs, query,
                PageRequest.of(page, size)); // ordering lives in the query: urgent + oldest first
        return PageResponse.of(result.map(DtoMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public AdminTicketDetail getTicket(Long id) {
        return detail(find(id));
    }

    @Transactional(readOnly = true)
    public AuthorService.DownloadableAttachment getAttachment(Long ticketId, Long attachmentId) {
        find(ticketId);
        TicketAttachment attachment = attachments.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> ResourceNotFoundException.of("Attachment", attachmentId));
        return new AuthorService.DownloadableAttachment(attachmentStorage.resolve(attachment.getStorageKey()),
                attachment.getFileName(), attachment.getContentType());
    }

    @Transactional(readOnly = true)
    public List<AdminUser> listAdmins() {
        return users.findAllByRoleOrderByNameAsc(Role.ADMIN).stream()
                .map(user -> new AdminUser(user.getId(), user.getName(), user.getEmail()))
                .toList();
    }

    // ------------------------------------------------------------------ management

    public AdminTicketDetail updateStatus(Long id, TicketStatus status) {
        Ticket t = find(id);
        t.setStatus(status);
        t.touch(); // surfaces the status change to the author's poll
        notifyAuthor(t);
        return detail(t);
    }

    /** Human override of the AI category; later AI re-triage will not change it. */
    public AdminTicketDetail overrideCategory(Long id, TicketCategory category) {
        Ticket t = find(id);
        t.overrideCategory(category);
        t.setAiDraft(null); // the draft was written against the old category's policy
        return detail(t);
    }

    public AdminTicketDetail overridePriority(Long id, Priority priority) {
        Ticket t = find(id);
        t.overridePriority(priority);
        return detail(t);
    }

    public AdminTicketDetail assignToMe(Long id, Long adminId) {
        Ticket t = find(id);
        t.setAssignedAdmin(users.getReferenceById(adminId));
        if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
        t.touch();
        notifyAuthor(t);
        return detail(t);
    }

    public AdminTicketDetail assign(Long id, Long adminId) {
        Ticket t = find(id);
        if (adminId == null) {
            t.setAssignedAdmin(null);
        } else {
            User admin = users.findById(adminId)
                    .filter(user -> user.getRole() == Role.ADMIN)
                    .orElseThrow(() -> ResourceNotFoundException.of("Admin", adminId));
            t.setAssignedAdmin(admin);
            if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
        }
        t.touch();
        notifyAuthor(t);
        return detail(t);
    }

    public AdminTicketDetail addNote(Long id, Long adminId, AddNoteRequest req) {
        Ticket t = find(id);
        notes.save(InternalNote.builder().ticket(t).admin(users.getReferenceById(adminId)).note(req.note().trim()).build());
        return detail(t); // no touch(): internal notes are invisible to the author
    }

    public AdminTicketDetail reply(Long id, Long adminId, AdminReplyRequest req) {
        Ticket t = find(id);
        if (t.getStatus() == TicketStatus.CLOSED && req.newStatus() == null) {
            throw new IllegalStateException("Ticket is closed. Reopen it (set a new status) before replying.");
        }
        User admin = users.getReferenceById(adminId);
        responses.save(TicketResponse.builder().ticket(t).sender(admin).senderRole(Role.ADMIN)
                .message(req.message().trim()).aiDrafted(req.aiDrafted()).build());
        if (t.getAssignedAdmin() == null) t.setAssignedAdmin(admin);
        if (req.newStatus() != null) t.setStatus(req.newStatus());
        else if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
        t.touch();
        notifyAuthor(t);
        return detail(t);
    }

    // ------------------------------------------------------------------ AI

    /**
     * Returns a cached draft if present, otherwise asks the LLM. NOT_SUPPORTED so no DB transaction/connection
     * is held open during the network call. AI failure is a normal, 200-level outcome (available=false).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiDraftResponse generateDraft(Long id, boolean regenerate) {
        Ticket t = find(id);
        if (!regenerate && t.getAiDraft() != null) {
            return new AiDraftResponse(true, t.getAiDraft(), null, true);
        }
        List<TicketResponse> thread = responses.findByTicketIdOrderByCreatedAtAsc(id);
        List<String> recent = thread.subList(Math.max(0, thread.size() - THREAD_MESSAGES_IN_PROMPT), thread.size()).stream()
                .map(r -> r.getSenderRole() + ": " + clip(r.getMessage()))
                .toList();
        AiService.DraftContext ctx = new AiService.DraftContext(t.getAuthor().getName(), t.getSubject(), t.getDescription(),
                t.getCategory(), bookSummary(t.getBook()), recent);
        try {
            String draft = ai.draft(ctx);
            tickets.saveAiDraft(id, draft);
            return new AiDraftResponse(true, draft, null, false);
        } catch (AiUnavailableException e) {
            return new AiDraftResponse(false, null,
                    "AI drafting is temporarily unavailable. You can write the response manually.", false);
        }
    }

    /** Re-runs triage, e.g. for tickets that fell back to rule-based triage during an outage. Respects overrides. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AdminTicketDetail retriage(Long id) {
        triage.triage(id);
        return detail(find(id)); // re-read: the merged entity returned by save() has uninitialised lazy associations
    }

    // ------------------------------------------------------------------ helpers

    /** Pushed to the author's open SSE connections after this transaction commits. */
    private void notifyAuthor(Ticket t) {
        events.publishEvent(new TicketUpdatedEvent(t.getAuthor().getId(), t.getId()));
    }

    private Ticket find(Long id) {
        return tickets.fetchById(id).orElseThrow(() -> ResourceNotFoundException.of("Ticket", id));
    }

    private AdminTicketDetail detail(Ticket t) {
        return DtoMapper.toDetail(t, responses.findByTicketIdOrderByCreatedAtAsc(t.getId()),
                notes.findByTicketIdOrderByCreatedAtAsc(t.getId()));
    }

    /** Compact, factual one-liner for the prompt; production-stage books are flagged explicitly. */
    private static String bookSummary(Book b) {
        if (b == null) return null;
        return "Title: " + b.getTitle()
                + "; ISBN: " + (b.getIsbn() == null ? "not yet assigned" : b.getIsbn())
                + "; Genre: " + nz(b.getGenre())
                + "; Status: " + nz(b.getStatus())
                + "; Publication date: " + (b.getPublicationDate() == null ? "not published yet" : b.getPublicationDate())
                + "; MRP: Rs " + nz(b.getMrp())
                + "; Copies sold: " + nz(b.getTotalCopiesSold())
                + "; Royalty earned: Rs " + nz(b.getTotalRoyaltyEarned())
                + "; Royalty paid: Rs " + nz(b.getRoyaltyPaid())
                + "; Royalty pending: Rs " + nz(b.getRoyaltyPending());
    }

    private static String nz(Object o) { return o == null ? "n/a" : o.toString(); }

    private static String clip(String s) {
        return s.length() <= THREAD_MESSAGE_MAX_CHARS ? s : s.substring(0, THREAD_MESSAGE_MAX_CHARS) + "...";
    }
}
