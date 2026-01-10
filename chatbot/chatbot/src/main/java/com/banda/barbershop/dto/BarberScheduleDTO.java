package com.banda.barbershop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberScheduleDTO {

    private Long barberId;
    private String barberName;
    private LocalDate date;
    private List<BookingSlot> bookings;
    private Integer totalBookings;
    private Double utilization; // Percentage of day booked (0-100)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingSlot {
        private String bookingCode;
        private LocalTime startTime;
        private LocalTime endTime;
        private String customerPhone;
        private String serviceName;
        private String status;
    }
}
