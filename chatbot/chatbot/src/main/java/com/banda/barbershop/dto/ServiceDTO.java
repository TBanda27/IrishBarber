package com.banda.barbershop.dto;

import com.banda.barbershop.entity.Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private Boolean active;
    private Integer displayOrder;

    public static ServiceDTO fromEntity(Service service) {
        return ServiceDTO.builder()
            .id(service.getId())
            .name(service.getName())
            .description(service.getDescription())
            .price(service.getPrice())
            .durationMinutes(service.getDurationMinutes())
            .active(service.isActive())
            .displayOrder(service.getDisplayOrder())
            .build();
    }

    public Service toEntity() {
        return Service.builder()
            .id(this.id)
            .name(this.name)
            .description(this.description)
            .price(this.price)
            .durationMinutes(this.durationMinutes)
            .active(this.active != null ? this.active : true)
            .displayOrder(this.displayOrder != null ? this.displayOrder : 0)
            .build();
    }
}
