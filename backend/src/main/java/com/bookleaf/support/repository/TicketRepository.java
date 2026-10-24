package com.bookleaf.support.repository;

import com.bookleaf.support.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends org.springframework.data.jpa.repository.JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = {"author", "book", "assignedAdmin"})
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> fetchById(@Param("id") Long id);

    /** Ownership-safe lookup: an author asking for someone else's ticket gets a 404, not a 403. */
    @EntityGraph(attributePaths = {"author", "book", "assignedAdmin"})
    @Query("select t from Ticket t where t.id = :id and t.author.id = :authorId")
    Optional<Ticket> fetchByIdAndAuthorId(@Param("id") Long id, @Param("authorId") Long authorId);

    /** Author's tickets, newest first; optional {@code since} makes polling cheap (only changed tickets). */
    @EntityGraph(attributePaths = {"book", "assignedAdmin"})
    @Query("""
            select t from Ticket t
            where t.author.id = :authorId
              and (:since is null or t.lastActivityAt > :since)
            order by t.createdAt desc
            """)
    List<Ticket> findForAuthor(@Param("authorId") Long authorId, @Param("since") LocalDateTime since);

    /** Admin queue: every filter optional; urgent + oldest active tickets first (see Ticket.queueRank). */
    @EntityGraph(attributePaths = {"author", "book", "assignedAdmin"})
    @Query("""
            select t from Ticket t
            where (:status is null or t.status = :status)
              and (:category is null or t.category = :category)
              and (:priority is null or t.priority = :priority)
              and (:assignedAdminId is null or t.assignedAdmin.id = :assignedAdminId)
              and (:fromDate is null or t.createdAt >= :fromDate)
              and (:toDate is null or t.createdAt < :toDate)
              and (:q is null or lower(t.subject) like lower(concat('%', :q, '%')))
            order by t.queueRank asc, t.createdAt asc
            """)
    Page<Ticket> search(@Param("status") TicketStatus status,
                        @Param("category") TicketCategory category,
                        @Param("priority") Priority priority,
                        @Param("assignedAdminId") Long assignedAdminId,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate,
                        @Param("q") String q,
                        Pageable pageable);

    /** Targeted update so caching a draft never overwrites a concurrent status/priority change. */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update Ticket t set t.aiDraft = :draft where t.id = :id")
    void saveAiDraft(@Param("id") Long id, @Param("draft") String draft);
}
