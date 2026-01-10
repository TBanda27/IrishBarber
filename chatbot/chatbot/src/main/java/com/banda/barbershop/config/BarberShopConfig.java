package com.banda.barbershop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "barbershop")
@Data
public class BarberShopConfig {

    private String name;
    private String address;
    private String phone;
    private String whatsappNumber;

    private Hours operatingHours;
    private Integer numberOfBarbers;
    private Integer slotIntervalMinutes;
    private Integer minimumAdvanceBookingHours;

    @Data
    public static class Hours {
        private LocalTime openingTime;
        private LocalTime closingTime;
        private List<DayOfWeek> closedDays;
    }

    public boolean isOpenOn(DayOfWeek day) {
        return operatingHours.closedDays == null ||
               !operatingHours.closedDays.contains(day);
    }

    public LocalTime getOpeningTime() {
        return operatingHours.openingTime;
    }

    public LocalTime getClosingTime() {
        return operatingHours.closingTime;
    }
}
