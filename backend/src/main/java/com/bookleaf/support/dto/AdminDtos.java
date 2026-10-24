package com.bookleaf.support.dto;

import com.bookleaf.support.entity.Priority;
import com.bookleaf.support.entity.TicketCategory;
import com.bookleaf.support.entity.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {}

    public record TicketSummary(Long id, Long authorId, String authorName, String authorEmail, Long bookId, String bookTitle,
                                String subject, TicketCategory category, String categoryLabel,
                                Priority priority, TicketStatus status, String statusLabel,
                                Long assignedAdminId, String assignedAdminName,
                                boolean aiClassified, boolean categoryOverridden, boolean priorityOverridden,
                                LocalDateTime createdAt, LocalDateTime lastActivityAt, long ageHours) {}

    public record AdminUser(Long id, String name, String email) {}

    public record CreateAdminInviteRequest(@NotBlank @Email @Size(max = 190) String email) {}

    public record AdminInviteResponse(String token, LocalDateTime expiresAt) {}

    public record AdminMessage(Long id, String senderName, String senderRole, String message,
                               boolean aiDrafted, LocalDateTime createdAt) {}

    public record NoteView(Long id, String adminName, String note, LocalDateTime createdAt) {}

    public record AdminTicketDetail(TicketSummary ticket, String description, String attachmentUrl, List<String> attachmentUrls,
                                    List<AuthorDtos.AttachmentView> attachments, String aiReasoning,
                                    boolean hasAiDraft, AuthorDtos.BookResponse book,
                                    List<AdminMessage> responses, List<NoteView> internalNotes) {}

    public record UpdateStatusRequest(@NotNull TicketStatus status) {}

    public record UpdateCategoryRequest(@NotNull TicketCategory category) {}

    public record UpdatePriorityRequest(@NotNull Priority priority) {}

    public record UpdateAssigneeRequest(Long adminId) {}

    public record AddNoteRequest(@NotBlank @Size(max = 3000) String note) {}

    /** aiDrafted = true when the admin started from the AI draft (even if edited). newStatus optional. */
    public record AdminReplyRequest(@NotBlank @Size(max = 5000) String message, boolean aiDrafted, TicketStatus newStatus) {}

    /** available=false means the LLM could not be used; the admin simply writes the reply manually. */
    public record AiDraftResponse(boolean available, String draft, String message, boolean cached) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}
