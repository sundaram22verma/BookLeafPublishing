package com.bookleaf.support.entity;

public enum TicketStatus {
    OPEN("Open", true),
    IN_PROGRESS("In Progress", true),
    RESOLVED("Resolved", false),
    CLOSED("Closed", false);

    private final String label;
    private final boolean active;

    TicketStatus(String label, boolean active) { this.label = label; this.active = active; }

    public String label() { return label; }

    /** Active tickets are the ones that need work and are sorted to the top of the admin queue. */
    public boolean isActive() { return active; }
}
