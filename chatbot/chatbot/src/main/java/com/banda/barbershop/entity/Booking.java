package com.banda.barbershop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings",
    uniqueConstraints = @UniqueConstraint(columnNames = "bookingCode"),
    indexes = {
        @Index(name = "idx_booking_date_status", columnList = "bookingDate,status"),
        @Index(name = "idx_customer_phone", columnList = "customerPhone"),
        @Index(name = "idx_booking_code", columnList = "bookingCode"),
        @Index(name = "idx_barber_date", columnList = "barber_id,bookingDate")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String bookingCode;

    @Column(nullable = false)
    private String customerPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id")
    private Barber barber;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime completedAt;

    // Reminder tracking
    @Column(nullable = false)
    private boolean dayBeforeReminderSent = false;

    @Column(nullable = false)
    private boolean oneHourReminderSent = false;

    private LocalDateTime dayBeforeReminderSentAt;

    private LocalDateTime oneHourReminderSentAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BookingStatus.CONFIRMED;
        }
    }

    public enum BookingStatus {
        CONFIRMED,
        CANCELLED,
        COMPLETED,
        NO_SHOW
    }
}
