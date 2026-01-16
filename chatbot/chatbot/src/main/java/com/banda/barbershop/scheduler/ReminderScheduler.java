package com.banda.barbershop.scheduler;

import com.banda.barbershop.config.ReminderConfig;
import com.banda.barbershop.service.ReminderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for sending automated booking reminders
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final ReminderService reminderService;
    private final ReminderConfig reminderConfig;

    @PostConstruct
    public void init() {
        log.info("=== Reminder Scheduler Initialized ===");
        log.info("Reminders enabled: {}", reminderConfig.isEnabled());
        log.info("Day-before reminders enabled: {}", reminderConfig.getDayBefore().isEnabled());
        log.info("Day-before time: {}", reminderConfig.getDayBefore().getTime());
        log.info("One-hour reminders enabled: {}", reminderConfig.getOneHour().isEnabled());
        log.info("One-hour minutes before: {}", reminderConfig.getOneHour().getMinutesBefore());
        log.info("=====================================");
    }

    /**
     * Send one-hour reminders
     * Runs every 10 minutes to catch appointments happening soon
     */
    @Scheduled(fixedRate = 600000) // Every 10 minutes (600,000 ms)
    public void sendOneHourReminders() {
        log.info("Running one-hour reminder job...");
        try {
            int sent = reminderService.sendOneHourReminders();
            log.info("One-hour reminder job completed: {} reminders sent", sent);
        } catch (Exception e) {
            log.error("Error in one-hour reminder job: {}", e.getMessage(), e);
        }
    }

    /**
     * Send day-before reminders
     * Runs at 6:00 PM daily (18:00)
     */
    @Scheduled(cron = "0 0 18 * * *") // Daily at 6 PM
    public void sendDayBeforeReminders() {
        log.info("Running day-before reminder job...");
        try {
            int sent = reminderService.sendDayBeforeReminders();
            log.info("Day-before reminder job completed: {} reminders sent", sent);
        } catch (Exception e) {
            log.error("Error in day-before reminder job: {}", e.getMessage(), e);
        }
    }
}
