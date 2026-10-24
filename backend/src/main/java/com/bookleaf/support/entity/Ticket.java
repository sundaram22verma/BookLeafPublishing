package com.bookleaf.support.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_author", columnList = "author_id"),
        @Index(name = "idx_ticket_queue", columnList = "queue_rank, created_at"),
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_category", columnList = "category"),
        @Index(name = "idx_ticket_priority", columnList = "priority"),
        @Index(name = "idx_ticket_assignee", columnList = "assigned_admin_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Null means "General / Account Level". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private TicketCategory category = TicketCategory.GENERAL_INQUIRY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @ElementCollection
    @CollectionTable(name = "ticket_attachment_links", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "attachment_url", length = 500, nullable = false)
    @OrderColumn(name = "link_order")
    @Builder.Default
    private List<String> attachmentUrls = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<TicketAttachment> attachments = new ArrayList<>();

    /** True only when the current category/priority came from the LLM (false = rule-based fallback). */
    @Column(name = "ai_classified", nullable = false)
    private boolean aiClassified;

    @Column(name = "ai_reasoning", length = 500)
    private String aiReasoning;

    @Column(name = "category_overridden", nullable = false)
    private boolean categoryOverridden;

    @Column(name = "priority_overridden", nullable = false)
    private boolean priorityOverridden;

    /** Cached AI draft so reopening a ticket does not cost another LLM call. Cleared on new author messages. */
    @Column(name = "ai_draft", columnDefinition = "TEXT")
    private String aiDraft;

    /**
     * Denormalised sort key for the admin queue: priority rank for active tickets, +10 for
     * resolved/closed ones. "ORDER BY queue_rank, created_at" = most urgent, then oldest, first,
     * and finished work sinks to the bottom. Indexed.
     */
    @Column(name = "queue_rank", nullable = false)
    private int queueRank;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Bumped on every visible change (reply, status change); powers the author's near-real-time polling. */
    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @PrePersist
    void onCreate() {
        if (lastActivityAt == null) lastActivityAt = LocalDateTime.now();
        recomputeQueueRank();
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
        recomputeQueueRank();
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
        recomputeQueueRank();
    }

    public void touch() { this.lastActivityAt = LocalDateTime.now(); }

    public void overrideCategory(TicketCategory category) {
        this.category = category;
        this.categoryOverridden = true;
    }

    public void overridePriority(Priority priority) {
        setPriority(priority);
        this.priorityOverridden = true;
    }

    /** Applies AI (or fallback) triage but never clobbers a human override. */
    public void applyTriage(TicketCategory category, Priority priority, String reasoning, boolean fromAi) {
        if (!categoryOverridden) this.category = category;
        if (!priorityOverridden) setPriority(priority);
        this.aiReasoning = reasoning == null ? null : reasoning.substring(0, Math.min(reasoning.length(), 500));
        this.aiClassified = fromAi;
    }

    private void recomputeQueueRank() {
        if (status != null && priority != null) {
            queueRank = (status.isActive() ? 0 : 10) + priority.rank();
        }
    }
}
