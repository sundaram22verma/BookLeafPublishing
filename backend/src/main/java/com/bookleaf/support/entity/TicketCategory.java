package com.bookleaf.support.entity;

public enum TicketCategory {
    ROYALTY_PAYMENTS("Royalty & Payments"),
    ISBN_METADATA("ISBN & Metadata Issues"),
    PRINTING_QUALITY("Printing & Quality"),
    DISTRIBUTION("Distribution & Availability"),
    PRODUCTION_STATUS("Book Status & Production Updates"),
    GENERAL_INQUIRY("General Inquiry");

    private final String label;

    TicketCategory(String label) { this.label = label; }

    public String label() { return label; }
}
