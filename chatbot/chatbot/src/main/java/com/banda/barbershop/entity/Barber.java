package com.banda.barbershop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "barbers", indexes = {
    @Index(name = "idx_barber_active", columnList = "active"),
    @Index(name = "idx_barber_phone", columnList = "phoneNumber")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Barber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private Double rating;

    @Builder.Default
    private Integer totalRatings = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalBookings = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer completedBookings = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
        if (this.totalBookings == null) {
            this.totalBookings = 0;
        }
        if (this.completedBookings == null) {
            this.completedBookings = 0;
        }
        if (this.totalRatings == null) {
            this.totalRatings = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
