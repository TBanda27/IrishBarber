package com.banda.barbershop.dto;

import com.banda.barbershop.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long id;
    private String bookingCode;
    private String customerPhone;
    private String serviceName;
    private Integer serviceDuration;
    private Double servicePrice;
    private Long barberId;
    private String barberName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;

    public static BookingDTO fromEntity(Booking booking) {
        return BookingDTO.builder()
            .id(booking.getId())
            .bookingCode(booking.getBookingCode())
            .customerPhone(booking.getCustomerPhone())
            .serviceName(booking.getService().getName())
            .serviceDuration(booking.getService().getDurationMinutes())
            .servicePrice(booking.getService().getPrice().doubleValue())
            .barberId(booking.getBarber() != null ? booking.getBarber().getId() : null)
            .barberName(booking.getBarber() != null ? booking.getBarber().getName() : "Not Assigned")
            .bookingDate(booking.getBookingDate())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .status(booking.getStatus().name())
            .build();
    }
}
