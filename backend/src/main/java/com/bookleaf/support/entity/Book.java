package com.bookleaf.support.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "books", indexes = @Index(name = "idx_book_author", columnList = "author_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    /** Null for books that have not reached the ISBN Assignment stage yet. */
    @Column(length = 32)
    private String isbn;

    private String genre;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    /** Free text so the dataset's production-stage values (e.g. "Typesetting") are kept as-is. */
    @Column(length = 80)
    private String status;

    @Column(precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(name = "total_copies_sold")
    private Integer totalCopiesSold;

    @Column(name = "total_royalty_earned", precision = 14, scale = 2)
    private BigDecimal totalRoyaltyEarned;

    @Column(name = "royalty_paid", precision = 14, scale = 2)
    private BigDecimal royaltyPaid;

    @Column(name = "royalty_pending", precision = 14, scale = 2)
    private BigDecimal royaltyPending;
}
