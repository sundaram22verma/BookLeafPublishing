package com.bookleaf.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AuthorDtos {
    private AuthorDtos() {}

    public record BookResponse(Long id, String title, String isbn, String genre, LocalDate publicationDate,
                               String status, BigDecimal mrp, Integer totalCopiesSold,
                               BigDecimal totalRoyaltyEarned, BigDecimal royaltyPaid, BigDecimal royaltyPending) {}

    /** bookId = null means "General / Account Level". */
    public record CreateTicketRequest(
            Long bookId,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(min = 10, max = 5000, message = "please describe the issue in 10-5000 characters") String description,
            @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "must be an http or https URL") String attachmentUrl,
            @Size(max = 10) List<@NotBlank @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "must be an http or https URL") String> attachmentUrls) {}

    public record ReplyRequest(@NotBlank @Size(max = 5000) String message) {}

    public record AttachmentView(Long id, String fileName, String downloadUrl) {}

    public record TicketMessage(Long id, String senderName, String senderRole, String message, LocalDateTime createdAt) {}

    /** Author-facing view: no priority, no AI fields, no internal notes. */
    public record AuthorTicketView(Long id, Long bookId, String bookTitle, String subject, String description,
                                   String status, String statusLabel, String attachmentUrl, List<String> attachmentUrls,
                                   List<AttachmentView> attachments,
                                   LocalDateTime createdAt, LocalDateTime lastActivityAt,
                                   List<TicketMessage> responses) {}
}
