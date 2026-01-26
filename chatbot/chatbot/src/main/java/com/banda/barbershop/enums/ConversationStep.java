package com.banda.barbershop.enums;

/**
 * State machine for barbershop booking conversation flow
 * Flow: MAIN_MENU → SELECT_SERVICE → SELECT_BARBER → SELECT_DATE → SELECT_TIME → CONFIRM_BOOKING → BOOKING_CONFIRMED
 */
public enum ConversationStep {
    // Main entry point
    MAIN_MENU,

    // Information
    VIEW_SERVICES,              // Show list of services and prices
    FAQ,                        // Frequently Asked Questions

    // Booking flow
    SELECT_SERVICE,             // Customer chooses haircut type
    SELECT_BARBER,              // Customer chooses their barber
    SELECT_DATE,                // Show available dates for next 7 days
    SELECT_TIME,                // Show available time slots for selected date
    CONFIRM_BOOKING,            // Confirm booking details before saving
    BOOKING_CONFIRMED,          // Booking created successfully

    // Booking management
    VIEW_MY_BOOKINGS,           // List customer's active bookings
    CANCEL_BOOKING_INPUT,       // Customer enters booking code to cancel
    CANCEL_BOOKING_CONFIRM;     // Confirm cancellation

    /**
     * Get parent state for "back" navigation
     */
    public ConversationStep getParentState() {
        return switch(this) {
            case VIEW_SERVICES -> MAIN_MENU;
            case FAQ -> MAIN_MENU;
            case SELECT_SERVICE -> MAIN_MENU;
            case SELECT_BARBER -> SELECT_SERVICE;
            case SELECT_DATE -> SELECT_BARBER;
            case SELECT_TIME -> SELECT_DATE;
            case CONFIRM_BOOKING -> SELECT_TIME;
            case BOOKING_CONFIRMED -> MAIN_MENU;
            case VIEW_MY_BOOKINGS -> MAIN_MENU;
            case CANCEL_BOOKING_INPUT -> VIEW_MY_BOOKINGS;
            case CANCEL_BOOKING_CONFIRM -> CANCEL_BOOKING_INPUT;
            default -> MAIN_MENU;
        };
    }

    /**
     * Check if state requires context data
     */
    public boolean requiresContext() {
        return this == SELECT_BARBER
            || this == SELECT_DATE
            || this == SELECT_TIME
            || this == CONFIRM_BOOKING
            || this == BOOKING_CONFIRMED
            || this == CANCEL_BOOKING_CONFIRM;
    }
}
