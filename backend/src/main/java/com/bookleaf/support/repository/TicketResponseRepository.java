package com.bookleaf.support.repository;

import com.bookleaf.support.entity.TicketResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TicketResponseRepository extends JpaRepository<TicketResponse, Long> {

    @EntityGraph(attributePaths = "sender")
    List<TicketResponse> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /** Batch fetch for the author's ticket list (avoids N+1). */
    @EntityGraph(attributePaths = "sender")
    List<TicketResponse> findByTicketIdInOrderByCreatedAtAsc(Collection<Long> ticketIds);
}
