package com.banda.barbershop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberStatsDTO {

    private Long barberId;
    private String barberName;
    private Integer totalBookings;
    private Integer completedBookings;
    private Integer recentBookings; // Last 30 bookings count
    private Double rating;
    private Double completionRate; // Percentage (0-100)
}
