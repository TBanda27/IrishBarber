package com.banda.barbershop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "loyalty")
@Data
public class LoyaltyConfig {

    private boolean enabled = true;

    // Points system
    private Integer pointsPerBooking = 10;
    private Integer bonusPointsForFirstBooking = 50;

    // Milestones (congratulate customers at these booking counts)
    private List<Integer> milestones = List.of(5, 10, 25, 50, 100);

    // Birthday rewards
    private Birthday birthday;

    @Data
    public static class Birthday {
        private boolean enabled = true;
        private Integer bonusPoints = 100;
        private String discountCode = "BDAY20"; // 20% off
        private Integer discountPercent = 20;
    }

    /**
     * Check if a booking count is a milestone
     */
    public boolean isMilestone(int bookingCount) {
        return milestones.contains(bookingCount);
    }

    /**
     * Get congratulations message for milestone
     */
    public String getMilestoneMessage(int bookingCount) {
        return switch (bookingCount) {
            case 5 -> "You've completed 5 bookings! You're becoming a regular! 🌟";
            case 10 -> "10 bookings! You're officially part of the family! 🎉";
            case 25 -> "25 bookings! You're a VIP customer! 👑";
            case 50 -> "50 bookings! Half-century milestone! 🏆";
            case 100 -> "100 bookings! Century club member! You're a legend! 🎖️";
            default -> String.format("You've completed %d bookings! Thank you for your loyalty! ⭐", bookingCount);
        };
    }
}
