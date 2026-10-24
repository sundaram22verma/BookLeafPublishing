package com.bookleaf.support.entity;

public enum Priority {
    CRITICAL(0), HIGH(1), MEDIUM(2), LOW(3);

    private final int rank;

    Priority(int rank) { this.rank = rank; }

    /** Lower rank = more urgent. */
    public int rank() { return rank; }
}
