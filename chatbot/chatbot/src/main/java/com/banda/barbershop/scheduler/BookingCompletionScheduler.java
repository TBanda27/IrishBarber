package com.banda.barbershop.scheduler;

import com.banda.barbershop.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler to auto-complete bookings after their appointment time has passed.
 * Runs every 15 minutes during business hours.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCompletionScheduler {

    private final BookingService bookingService;

    /**
     * Auto-complete bookings that have passed their end time.
     * Runs every 15 minutes from 9 AM to 8 PM.
     */
    @Scheduled(cron = "0 */15 9-20 * * *")
    public void processCompletedBookings() {
        log.debug("Running booking completion scheduler...");
        try {
            int completed = bookingService.autoCompleteBookings();
            if (completed > 0) {
                log.info("Booking completion scheduler processed {} bookings", completed);
            }
        } catch (Exception e) {
            log.error("Error in booking completion scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * End-of-day cleanup: mark any remaining confirmed bookings from today as completed.
     * Runs at 9 PM every day.
     */
    @Scheduled(cron = "0 0 21 * * *")
    public void endOfDayCompletion() {
        log.info("Running end-of-day booking completion...");
        try {
            int completed = bookingService.autoCompleteBookings();
            log.info("End-of-day completion processed {} bookings", completed);
        } catch (Exception e) {
            log.error("Error in end-of-day completion: {}", e.getMessage(), e);
        }
    }
}
