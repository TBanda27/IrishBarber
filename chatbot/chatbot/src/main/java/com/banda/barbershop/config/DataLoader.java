package com.banda.barbershop.config;

import com.banda.barbershop.entity.Barber;
import com.banda.barbershop.entity.Service;
import com.banda.barbershop.repository.BarberRepository;
import com.banda.barbershop.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Automatically seeds initial services data when the application starts
 * Only seeds if the services table is empty
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final ServiceRepository serviceRepository;
    private final BarberRepository barberRepository;

    @Override
    public void run(String... args) {
        // Seed services
        if (serviceRepository.count() == 0) {
            log.info("No services found in database. Seeding initial services...");
            seedServices();
            log.info("Successfully seeded {} services", serviceRepository.count());
        } else {
            log.info("Services already exist in database. Skipping seed.");
        }

        // Seed barbers
        if (barberRepository.count() == 0) {
            log.info("No barbers found in database. Seeding initial barbers...");
            seedBarbers();
            log.info("Successfully seeded {} barbers", barberRepository.count());
        } else {
            log.info("Barbers already exist in database. Skipping seed.");
        }
    }

    private void seedServices() {
        Service standardCut = Service.builder()
            .name("Standard Cut")
            .description("Classic haircut with styling")
            .price(new BigDecimal("25.00"))
            .durationMinutes(30)
            .active(true)
            .displayOrder(1)
            .build();

        Service skinFade = Service.builder()
            .name("Skin Fade")
            .description("Precision fade with clean lines")
            .price(new BigDecimal("30.00"))
            .durationMinutes(45)
            .active(true)
            .displayOrder(2)
            .build();

        Service beardTrim = Service.builder()
            .name("Beard Trim")
            .description("Shape and trim your beard")
            .price(new BigDecimal("15.00"))
            .durationMinutes(20)
            .active(true)
            .displayOrder(3)
            .build();

        Service cutAndBeard = Service.builder()
            .name("Cut & Beard")
            .description("Full service haircut and beard trim")
            .price(new BigDecimal("40.00"))
            .durationMinutes(60)
            .active(true)
            .displayOrder(4)
            .build();

        serviceRepository.save(standardCut);
        serviceRepository.save(skinFade);
        serviceRepository.save(beardTrim);
        serviceRepository.save(cutAndBeard);

        log.info("Seeded services: Standard Cut, Skin Fade, Beard Trim, Cut & Beard");
    }

    private void seedBarbers() {
        Barber mike = Barber.builder()
            .name("Mike")
            .phoneNumber("+353871234501")
            .active(true)
            .displayOrder(1)
            .bio("Senior barber with 10 years experience. Specializes in classic cuts and fades.")
            .rating(4.8)
            .totalRatings(120)
            .totalBookings(0)
            .completedBookings(0)
            .build();

        Barber john = Barber.builder()
            .name("John")
            .phoneNumber("+353871234502")
            .active(true)
            .displayOrder(2)
            .bio("Expert in modern fades and creative designs. Loves experimenting with new styles.")
            .rating(4.6)
            .totalRatings(85)
            .totalBookings(0)
            .completedBookings(0)
            .build();

        Barber steve = Barber.builder()
            .name("Steve")
            .phoneNumber("+353871234503")
            .active(true)
            .displayOrder(3)
            .bio("Master of traditional barbering. Expert in straight razor shaves and beard grooming.")
            .rating(4.7)
            .totalRatings(95)
            .totalBookings(0)
            .completedBookings(0)
            .build();

        Barber alex = Barber.builder()
            .name("Alex")
            .phoneNumber("+353871234504")
            .active(true)
            .displayOrder(4)
            .bio("Young talent with fresh ideas. Great with kids and trendy cuts.")
            .rating(4.5)
            .totalRatings(60)
            .totalBookings(0)
            .completedBookings(0)
            .build();

        barberRepository.save(mike);
        barberRepository.save(john);
        barberRepository.save(steve);
        barberRepository.save(alex);

        log.info("Seeded barbers: Mike, John, Steve, Alex");
    }
}
