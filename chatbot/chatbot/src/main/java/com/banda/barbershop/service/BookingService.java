package com.banda.barbershop.service;

import com.banda.barbershop.entity.Barber;
import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.entity.Booking.BookingStatus;
import com.banda.barbershop.entity.Service;
import com.banda.barbershop.repository.BarberRepository;
import com.banda.barbershop.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BarberRepository barberRepository;
    private final AvailabilityService availabilityService;
    private final CustomerService customerService;
    private static final Random RANDOM = new Random();

    /**
     * Create a new booking with specific barber
     */
    @Transactional
    public Booking createBooking(String customerPhone, Service service, Long barberId,
                                  LocalDate bookingDate, LocalTime startTime) {

        // Fetch barber entity
        Barber barber = barberRepository.findById(barberId)
            .orElseThrow(() -> new IllegalStateException("Barber not found"));

        // Validate barber-specific slot availability
        if (!availabilityService.validateBarberSlotAvailability(bookingDate, startTime, service, barberId)) {
            throw new IllegalStateException("Selected time slot is no longer available for this barber");
        }

        // Calculate end time
        LocalTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        // Generate unique booking code
        String bookingCode = generateUniqueBookingCode();

        // Create booking
        Booking booking = Booking.builder()
            .bookingCode(bookingCode)
            .customerPhone(customerPhone)
            .service(service)
            .barber(barber)
            .bookingDate(bookingDate)
            .startTime(startTime)
            .endTime(endTime)
            .status(BookingStatus.CONFIRMED)
            .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Created booking {} for customer {} with barber {} on {} at {}",
                 bookingCode, customerPhone, barber.getName(), bookingDate, startTime);

        // Update barber statistics
        updateBarberStats(barberId);

        // Update customer profile and award loyalty points
        customerService.recordBooking(customerPhone, service, barberId);

        return savedBooking;
    }

    /**
     * Update barber statistics after successful booking
     */
    private void updateBarberStats(Long barberId) {
        barberRepository.findById(barberId).ifPresent(barber -> {
            barber.setTotalBookings(barber.getTotalBookings() + 1);
            barberRepository.save(barber);
            log.debug("Updated stats for barber {}: {} total bookings",
                     barber.getName(), barber.getTotalBookings());
        });
    }

    /**
     * Get customer's active bookings
     */
    public List<Booking> getCustomerBookings(String customerPhone) {
        List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED);
        List<Booking> bookings = bookingRepository.findByCustomerPhoneAndStatusIn(
            customerPhone, activeStatuses);

        log.debug("Found {} active bookings for customer {}", bookings.size(), customerPhone);
        return bookings;
    }

    /**
     * Cancel a booking by booking code
     */
    @Transactional
    public boolean cancelBooking(String bookingCode, String customerPhone) {
        Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode);

        if (bookingOpt.isEmpty()) {
            log.warn("Booking code {} not found", bookingCode);
            return false;
        }

        Booking booking = bookingOpt.get();

        // Verify ownership
        if (!booking.getCustomerPhone().equals(customerPhone)) {
            log.warn("Customer {} attempted to cancel booking {} owned by {}",
                     customerPhone, bookingCode, booking.getCustomerPhone());
            return false;
        }

        // Check if already cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Booking {} is already cancelled", bookingCode);
            return false;
        }

        // Cancel the booking
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Update customer cancellation stats
        customerService.recordCancelledBooking(customerPhone);

        log.info("Cancelled booking {} for customer {}", bookingCode, customerPhone);
        return true;
    }

    /**
     * Get booking by code
     */
    public Optional<Booking> getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode);
    }

    /**
     * Generate a unique booking code (e.g., BK8472)
     */
    private String generateUniqueBookingCode() {
        String code;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            code = "BK" + String.format("%04d", RANDOM.nextInt(10000));
            attempts++;

            if (attempts >= maxAttempts) {
                // Use timestamp as fallback
                code = "BK" + System.currentTimeMillis() % 10000;
                break;
            }
        } while (bookingRepository.findByBookingCode(code).isPresent());

        return code;
    }

    /**
     * Check if customer has any upcoming bookings for the same service on the same day
     */
    public boolean hasDuplicateBooking(String customerPhone, Service service, LocalDate date) {
        List<Booking> bookings = bookingRepository.findByBookingDateAndStatus(
            date, BookingStatus.CONFIRMED);

        return bookings.stream()
            .anyMatch(b -> b.getCustomerPhone().equals(customerPhone) &&
                          b.getService().getId().equals(service.getId()));
    }
}
