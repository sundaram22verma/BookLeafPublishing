package com.bookleaf.support.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** One message in the author <-> admin conversation on a ticket. */
@Entity
@Table(name = "ticket_responses", indexes = @Index(name = "idx_response_ticket", columnList = "ticket_id, created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 20)
    private Role senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Whether the admin started from an AI draft (kept for draft-acceptance analytics). */
    @Column(name = "is_ai_drafted", nullable = false)
    private boolean aiDrafted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
