package com.banda.barbershop.dto;

import com.banda.barbershop.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private Long id;
    private String phoneNumber;
    private String name;
    private LocalDate birthday;
    private LocalDate firstVisit;
    private LocalDate lastVisit;
    private Integer totalBookings;
    private Integer completedBookings;
    private Integer loyaltyPoints;
    private String preferredService;

    public static CustomerDTO fromEntity(Customer customer) {
        return CustomerDTO.builder()
            .id(customer.getId())
            .phoneNumber(customer.getPhoneNumber())
            .name(customer.getName())
            .birthday(customer.getBirthday())
            .firstVisit(customer.getFirstVisit())
            .lastVisit(customer.getLastVisit())
            .totalBookings(customer.getTotalBookings())
            .completedBookings(customer.getCompletedBookings())
            .loyaltyPoints(customer.getLoyaltyPoints())
            .preferredService(customer.getPreferredService() != null ?
                customer.getPreferredService().getName() : null)
            .build();
    }
}
