package com.banda.barbershop.controller;

import com.banda.barbershop.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test endpoints for manually triggering reminders
 * Useful for testing and debugging
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Slf4j
public class ReminderTestController {

    private final ReminderService reminderService;

    /**
     * Manually trigger day-before reminders
     * GET /api/reminders/day-before
     */
    @PostMapping("/day-before")
    public ResponseEntity<?> triggerDayBeforeReminders() {
        log.info("Manual trigger: day-before reminders");
        try {
            int sent = reminderService.sendDayBeforeReminders();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Day-before reminders triggered",
                "sent", sent
            ));
        } catch (Exception e) {
            log.error("Error triggering day-before reminders", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Manually trigger one-hour reminders
     * POST /api/reminders/one-hour
     */
    @PostMapping("/one-hour")
    public ResponseEntity<?> triggerOneHourReminders() {
        log.info("Manual trigger: one-hour reminders");
        try {
            int sent = reminderService.sendOneHourReminders();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "One-hour reminders triggered",
                "sent", sent
            ));
        } catch (Exception e) {
            log.error("Error triggering one-hour reminders", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Send test reminder for specific booking
     * POST /api/reminders/test/{bookingCode}
     */
    @PostMapping("/test/{bookingCode}")
    public ResponseEntity<?> sendTestReminder(@PathVariable String bookingCode) {
        log.info("Sending test reminder for booking: {}", bookingCode);
        try {
            reminderService.sendTestReminder(bookingCode);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test reminder sent for booking " + bookingCode
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error sending test reminder", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
}
