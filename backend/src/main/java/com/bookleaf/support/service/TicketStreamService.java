//package com.bookleaf.support.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.MediaType;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.event.TransactionPhase;
//import org.springframework.transaction.event.TransactionalEventListener;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArrayList;
//
///**
// * Server-Sent Events hub: one or more open connections per author (several browser tabs are fine).
// * Events are pushed only AFTER the admin's transaction commits, so a client that reacts to the event by
// * fetching the ticket always sees the new data. In-memory, so it works on a single instance; for several
// * instances put Redis pub/sub (or similar) behind {@link #push}.
// */
//@Slf4j
//@Service
//public class TicketStreamService {
//
//    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // browser reconnects automatically afterwards
//
//    public record TicketUpdatedPayload(Long ticketId) {}
//
//    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
//
//    public SseEmitter subscribe(Long authorId) {
//        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
//        emitters.computeIfAbsent(authorId, k -> new CopyOnWriteArrayList<>()).add(emitter);
//        Runnable cleanup = () -> remove(authorId, emitter);
//        emitter.onCompletion(cleanup);
//        emitter.onTimeout(cleanup);
//        emitter.onError(e -> cleanup.run());
//        send(authorId, emitter, SseEmitter.event().name("connected").data("ok"));
//        return emitter;
//    }
//
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void onTicketUpdated(TicketUpdatedEvent event) {
//        List<SseEmitter> list = emitters.get(event.authorId());
//        if (list == null) return;
//        for (SseEmitter emitter : list) {
//            send(event.authorId(), emitter, SseEmitter.event().name("ticket-updated")
//                    .data(new TicketUpdatedPayload(event.ticketId()), MediaType.APPLICATION_JSON));
//        }
//    }
//
//    /** Comment line every 25s keeps proxies/load balancers from closing idle connections, and prunes dead ones. */
//    @Scheduled(fixedRate = 25_000)
//    void heartbeat() {
//        emitters.forEach((authorId, list) -> list.forEach(e -> send(authorId, e, SseEmitter.event().comment("keepalive"))));
//    }
//
//    private void send(Long authorId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
//        try {
//            synchronized (emitter) { // SseEmitter is not safe for concurrent sends
//                emitter.send(event);
//            }
//        } catch (IOException | IllegalStateException e) {
//            remove(authorId, emitter); // client went away
//        }
//    }
//
//    private void remove(Long authorId, SseEmitter emitter) {
//        emitters.computeIfPresent(authorId, (k, list) -> {
//            list.remove(emitter);
//            return list.isEmpty() ? null : list;
//        });
//    }
//}


package com.bookleaf.support.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events hub: one or more open connections per author (several browser tabs are fine).
 * Events are pushed only AFTER the admin's transaction commits, so a client that reacts to the event by
 * fetching the ticket always sees the new data. In-memory, so it works on a single instance; for several
 * instances put Redis pub/sub (or similar) behind {@link #push}.
 */
@Slf4j
@Service
public class TicketStreamService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // browser reconnects automatically afterwards
    private static final int MAX_CONNECTIONS_PER_AUTHOR = 5; // stops one account exhausting server connections

    public record TicketUpdatedPayload(Long ticketId) {}

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long authorId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> mine = emitters.computeIfAbsent(authorId, k -> new CopyOnWriteArrayList<>());
        while (mine.size() >= MAX_CONNECTIONS_PER_AUTHOR) { // drop the oldest tab's stream; the client auto-reconnects
            SseEmitter oldest = mine.remove(0);
            try { oldest.complete(); } catch (RuntimeException ignored) { /* already closed */ }
        }
        mine.add(emitter);
        Runnable cleanup = () -> remove(authorId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        send(authorId, emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketUpdated(TicketUpdatedEvent event) {
        List<SseEmitter> list = emitters.get(event.authorId());
        if (list == null) return;
        for (SseEmitter emitter : list) {
            send(event.authorId(), emitter, SseEmitter.event().name("ticket-updated")
                    .data(new TicketUpdatedPayload(event.ticketId()), MediaType.APPLICATION_JSON));
        }
    }

    /** Comment line every 25s keeps proxies/load balancers from closing idle connections, and prunes dead ones. */
    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        emitters.forEach((authorId, list) -> list.forEach(e -> send(authorId, e, SseEmitter.event().comment("keepalive"))));
    }

    private void send(Long authorId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            synchronized (emitter) { // SseEmitter is not safe for concurrent sends
                emitter.send(event);
            }
        } catch (IOException | IllegalStateException e) {
            remove(authorId, emitter); // client went away
        }
    }

    private void remove(Long authorId, SseEmitter emitter) {
        emitters.computeIfPresent(authorId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}