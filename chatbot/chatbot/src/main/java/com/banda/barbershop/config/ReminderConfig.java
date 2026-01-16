package com.banda.barbershop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "reminders")
@Data
public class ReminderConfig {

    private boolean enabled = true;

    private DayBeforeReminder dayBefore = new DayBeforeReminder();
    private OneHourReminder oneHour = new OneHourReminder();

    @Data
    public static class DayBeforeReminder {
        private boolean enabled = true;
        private String time = "18:00"; // 6 PM the day before
    }

    @Data
    public static class OneHourReminder {
        private boolean enabled = true;
        private int minutesBefore = 60; // 1 hour before
    }
}
