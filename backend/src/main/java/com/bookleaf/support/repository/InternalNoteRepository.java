package com.bookleaf.support.repository;

import com.bookleaf.support.entity.InternalNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InternalNoteRepository extends JpaRepository<InternalNote, Long> {

    @EntityGraph(attributePaths = "admin")
    List<InternalNote> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
