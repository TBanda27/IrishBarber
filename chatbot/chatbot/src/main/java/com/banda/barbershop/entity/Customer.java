package com.banda.barbershop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers",
    indexes = {
        @Index(name = "idx_phone_number", columnList = "phoneNumber"),
        @Index(name = "idx_birthday_month", columnList = "birthdayMonth"),
        @Index(name = "idx_loyalty_points", columnList = "loyaltyPoints")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(length = 100)
    private String name;

    // Birthday tracking (for birthday messages)
    private LocalDate birthday;

    @Column(length = 2)
    private Integer birthdayMonth; // 1-12, for quick queries

    @Column(length = 2)
    private Integer birthdayDay; // 1-31, for quick queries

    private LocalDate lastBirthdayMessageSent; // Track yearly message

    // Visit history
    private LocalDate lastVisit;
    private LocalDate firstVisit;

    @Column(nullable = false)
    private Integer totalBookings = 0;

    @Column(nullable = false)
    private Integer completedBookings = 0;

    @Column(nullable = false)
    private Integer cancelledBookings = 0;

    @Column(nullable = false)
    private Integer noShowBookings = 0;

    // Preferences
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_service_id")
    private Service preferredService; // Most booked service

    private Integer preferredServiceCount = 0; // How many times they booked it

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_barber_id")
    private Barber preferredBarber; // Most booked barber

    private Integer preferredBarberCount = 0; // How many times they booked with this barber

    // Loyalty program
    @Column(nullable = false)
    private Integer loyaltyPoints = 0;

    private Integer lifetimeLoyaltyPoints = 0; // Total points earned (never decreases)

    private LocalDate lastLoyaltyRewardDate; // Track when last reward was given

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.totalBookings == null) this.totalBookings = 0;
        if (this.completedBookings == null) this.completedBookings = 0;
        if (this.cancelledBookings == null) this.cancelledBookings = 0;
        if (this.noShowBookings == null) this.noShowBookings = 0;
        if (this.loyaltyPoints == null) this.loyaltyPoints = 0;
        if (this.lifetimeLoyaltyPoints == null) this.lifetimeLoyaltyPoints = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if customer has a birthday this month
     */
    public boolean isBirthdayMonth() {
        return this.birthdayMonth != null &&
               this.birthdayMonth == LocalDate.now().getMonthValue();
    }

    /**
     * Check if today is customer's birthday
     */
    public boolean isBirthdayToday() {
        LocalDate today = LocalDate.now();
        return this.birthdayMonth != null &&
               this.birthdayDay != null &&
               this.birthdayMonth == today.getMonthValue() &&
               this.birthdayDay == today.getDayOfMonth();
    }

    /**
     * Check if birthday message was already sent this year
     */
    public boolean birthdayMessageSentThisYear() {
        if (lastBirthdayMessageSent == null) return false;
        return lastBirthdayMessageSent.getYear() == LocalDate.now().getYear();
    }
}
