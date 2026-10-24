package com.bookleaf.support.service;

import com.bookleaf.support.entity.Ticket;
import com.bookleaf.support.exception.ResourceNotFoundException;
import com.bookleaf.support.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Classifies + prioritises tickets after the creating transaction commits, off the request thread.
 * The author's POST therefore returns immediately, and an LLM outage can never block or fail ticket creation.
 * Human overrides are never overwritten (see Ticket.applyTriage). Deliberately not @Transactional:
 * no DB connection is held open while waiting on the LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketTriageService {

    private final TicketRepository tickets;
    private final AiService ai;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        try {
            triage(event.ticketId());
        } catch (Exception e) {
            // Never lose the ticket over triage; it keeps its defaults and admins can re-run triage.
            log.error("Triage failed for ticket {}", event.ticketId(), e);
        }
    }

    /** Also used by the admin "re-run AI triage" endpoint (e.g. after an outage). */
    public void triage(Long ticketId) {
        Ticket ticket = tickets.fetchById(ticketId).orElseThrow(() -> ResourceNotFoundException.of("Ticket", ticketId));
        AiService.TriageOutcome outcome = ai.triage(ticket.getSubject(), ticket.getDescription());
        ticket.applyTriage(outcome.category(), outcome.priority(), outcome.reasoning(), outcome.aiGenerated());
        tickets.save(ticket);
    }
}
