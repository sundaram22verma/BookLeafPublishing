package com.bookleaf.support.service;

/** Published (inside the admin's transaction) when something the author can see on a ticket changed. */
public record TicketUpdatedEvent(Long authorId, Long ticketId) {}