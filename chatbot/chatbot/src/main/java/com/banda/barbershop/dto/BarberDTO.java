package com.banda.barbershop.dto;

import com.banda.barbershop.entity.Barber;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberDTO {

    private Long id;
    private String name;
    private String phoneNumber;
    private Boolean active;
    private Integer displayOrder;
    private String bio;
    private Double rating;
    private Integer totalBookings;
    private Integer completedBookings;

    public static BarberDTO fromEntity(Barber barber) {
        return BarberDTO.builder()
            .id(barber.getId())
            .name(barber.getName())
            .phoneNumber(barber.getPhoneNumber())
            .active(barber.getActive())
            .displayOrder(barber.getDisplayOrder())
            .bio(barber.getBio())
            .rating(barber.getRating())
            .totalBookings(barber.getTotalBookings())
            .completedBookings(barber.getCompletedBookings())
            .build();
    }

    public Barber toEntity() {
        return Barber.builder()
            .id(this.id)
            .name(this.name)
            .phoneNumber(this.phoneNumber)
            .active(this.active != null ? this.active : true)
            .displayOrder(this.displayOrder != null ? this.displayOrder : 0)
            .bio(this.bio)
            .rating(this.rating)
            .totalBookings(this.totalBookings != null ? this.totalBookings : 0)
            .completedBookings(this.completedBookings != null ? this.completedBookings : 0)
            .build();
    }
}
