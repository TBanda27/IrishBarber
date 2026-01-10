package com.banda.barbershop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    // Today's stats
    private Integer todayBookings;
    private Integer todayCompleted;
    private Integer todayCancelled;
    private Integer todayNoShows;
    private Double todayRevenue;

    // Week stats
    private Integer weekBookings;
    private Double weekRevenue;

    // Month stats
    private Integer monthBookings;
    private Double monthRevenue;

    // Overall stats
    private Integer totalCustomers;
    private Integer activeCustomers;
    private Double averageBookingValue;

    // Most popular services
    private List<ServiceStatsDTO> popularServices;

    // Upcoming bookings count
    private Integer upcomingToday;
    private Integer upcomingTomorrow;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStatsDTO {
        private String serviceName;
        private Long bookingCount;
        private Double totalRevenue;
    }
}
