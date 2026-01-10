package com.banda.barbershop.enums;

/**
 * State machine for barbershop booking conversation flow
 * Flow: MAIN_MENU → SELECT_SERVICE → SELECT_BARBER → VIEW_SLOTS → CONFIRM_BOOKING → BOOKING_CONFIRMED
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
    VIEW_TODAY_SLOTS,           // Show available times for today
    VIEW_TOMORROW_SLOTS,        // Show available times for tomorrow (if today is full)
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
            case VIEW_TODAY_SLOTS, VIEW_TOMORROW_SLOTS -> SELECT_BARBER;
            case CONFIRM_BOOKING -> VIEW_TODAY_SLOTS;
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
            || this == VIEW_TODAY_SLOTS
            || this == VIEW_TOMORROW_SLOTS
            || this == CONFIRM_BOOKING
            || this == BOOKING_CONFIRMED
            || this == CANCEL_BOOKING_CONFIRM;
    }
}
