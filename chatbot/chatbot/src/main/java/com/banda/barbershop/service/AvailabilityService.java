package com.banda.barbershop.service;

import com.banda.barbershop.config.BarberShopConfig;
import com.banda.barbershop.entity.Service;
import com.banda.barbershop.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private final BarberShopConfig shopConfig;
    private final BookingRepository bookingRepository;

    /**
     * Get available time slots for specific barber on specific date
     */
    public List<LocalTime> getAvailableSlotsForBarber(Service service, Long barberId, LocalDate date) {
        if (!shopConfig.isOpenOn(date.getDayOfWeek())) {
            log.debug("Shop is closed on {}", date.getDayOfWeek());
            return List.of();
        }

        LocalTime fromTime;
        if (date.equals(LocalDate.now())) {
            // For today, check minimum advance booking
            LocalTime now = LocalTime.now();
            fromTime = now.plusHours(shopConfig.getMinimumAdvanceBookingHours());

            // If earliest slot wraps past midnight or is after closing, no slots available
            if (fromTime.isBefore(now) ||
                fromTime.isAfter(shopConfig.getClosingTime()) ||
                fromTime.equals(shopConfig.getClosingTime())) {
                log.debug("No more slots available today for barber {}", barberId);
                return List.of();
            }
        } else {
            // For future dates, start from opening time
            fromTime = shopConfig.getOpeningTime();
        }
        return getAvailableSlotsForBarber(date, service, barberId, fromTime);
    }

    /**
     * Get available dates for a barber within the next N days
     * Returns map of LocalDate -> slot count (preserves insertion order)
     */
    public Map<LocalDate, Integer> getAvailableDatesForBarber(Service service, Long barberId, int maxDays) {
        Map<LocalDate, Integer> availableDates = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < maxDays; i++) {
            LocalDate date = today.plusDays(i);

            // Skip closed days
            if (!shopConfig.isOpenOn(date.getDayOfWeek())) {
                log.debug("Skipping {} - shop is closed on {}", date, date.getDayOfWeek());
                continue;
            }

            List<LocalTime> slots = getAvailableSlotsForBarber(service, barberId, date);
            if (!slots.isEmpty()) {
                availableDates.put(date, slots.size());
                log.debug("Date {} has {} available slots for barber {}", date, slots.size(), barberId);
            }
        }

        log.info("Found {} available dates for barber {} in next {} days",
                 availableDates.size(), barberId, maxDays);
        return availableDates;
    }

    /**
     * Core logic to calculate available slots for specific barber
     */
    private List<LocalTime> getAvailableSlotsForBarber(LocalDate date, Service service,
                                                        Long barberId, LocalTime fromTime) {
        List<LocalTime> availableSlots = new ArrayList<>();

        LocalTime currentSlot = shopConfig.getOpeningTime();
        LocalTime closingTime = shopConfig.getClosingTime();
        Integer slotInterval = shopConfig.getSlotIntervalMinutes();
        Integer serviceDuration = service.getDurationMinutes();

        while (currentSlot.plusMinutes(serviceDuration).isBefore(closingTime) ||
               currentSlot.plusMinutes(serviceDuration).equals(closingTime)) {

            // Only consider slots after the minimum time
            if (currentSlot.isBefore(fromTime)) {
                currentSlot = currentSlot.plusMinutes(slotInterval);
                continue;
            }

            // Check if this barber is available at this slot
            if (isBarberAvailable(date, currentSlot, serviceDuration, barberId)) {
                availableSlots.add(currentSlot);
            }
            currentSlot = currentSlot.plusMinutes(slotInterval);
        }
        log.debug("Found {} available slots for barber {} on {} starting from {}",
                  availableSlots.size(), barberId, date, fromTime);
        return availableSlots;
    }
    /**
     * Check if specific barber is available at time slot (no overlapping bookings)
     */
    private boolean isBarberAvailable(LocalDate date, LocalTime slotTime,
                                       Integer serviceDuration, Long barberId) {
        LocalTime serviceEndTime = slotTime.plusMinutes(serviceDuration);

        // Check if barber has any overlapping bookings
        Long overlappingBookings = bookingRepository.countBarberBookingsAtSlot(
            barberId, date, slotTime, serviceEndTime);

        boolean available = overlappingBookings == 0;

        if (!available) {
            log.debug("Barber {} is busy at {} on {}", barberId, slotTime, date);
        }

        return available;
    }

    /**
     * Validate slot availability for specific barber (comprehensive validation)
     */
    public boolean validateBarberSlotAvailability(LocalDate date, LocalTime time,
                                                   Service service, Long barberId) {
        // Check if shop is open
        if (!shopConfig.isOpenOn(date.getDayOfWeek())) {
            log.warn("Attempted to book on closed day: {}", date.getDayOfWeek());
            return false;
        }

        // Check business hours
        LocalTime serviceEndTime = time.plusMinutes(service.getDurationMinutes());
        if (time.isBefore(shopConfig.getOpeningTime()) ||
            serviceEndTime.isAfter(shopConfig.getClosingTime())) {
            log.warn("Booking time {} is outside business hours", time);
            return false;
        }

        // Check if in the past
        LocalDateTime bookingDateTime = LocalDateTime.of(date, time);
        if (bookingDateTime.isBefore(LocalDateTime.now())) {
            log.warn("Cannot book in the past: {}", bookingDateTime);
            return false;
        }

        // Check minimum advance booking for today
        if (date.equals(LocalDate.now())) {
            LocalTime minimumTime = LocalTime.now().plusHours(
                shopConfig.getMinimumAdvanceBookingHours());
            if (time.isBefore(minimumTime)) {
                log.warn("Booking time {} is less than {} hours from now",
                         time, shopConfig.getMinimumAdvanceBookingHours());
                return false;
            }
        }

        // Check barber availability
        return isBarberAvailable(date, time, service.getDurationMinutes(), barberId);
    }

    /**
     * Get available time slots for today
     * Only shows slots that are at least minimumAdvanceBookingHours from now
     */
    public List<LocalTime> getAvailableSlotsForToday(Service service) {
        LocalDate today = LocalDate.now();

        if (!shopConfig.isOpenOn(today.getDayOfWeek())) {
            log.debug("Shop is closed on {}", today.getDayOfWeek());
            return List.of();
        }

        LocalTime now = LocalTime.now();
        LocalTime earliestSlot = now.plusHours(shopConfig.getMinimumAdvanceBookingHours());

        // If earliest available slot wraps to next day (past midnight), show no slots for today
        if (earliestSlot.isBefore(now)) {
            log.debug("Earliest slot ({}) has wrapped to next day. No more slots today.", earliestSlot);
            return List.of();
        }

        // If earliest slot is after closing time, no slots available today
        if (earliestSlot.isAfter(shopConfig.getClosingTime()) ||
            earliestSlot.equals(shopConfig.getClosingTime())) {
            log.debug("Earliest slot ({}) is after closing time ({}). No more slots today.",
                     earliestSlot, shopConfig.getClosingTime());
            return List.of();
        }

        return getAvailableSlots(today, service, earliestSlot);
    }

    /**
     * Get available time slots for tomorrow
     * Shows all slots from opening time
     */
    public List<LocalTime> getAvailableSlotsForTomorrow(Service service) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        if (!shopConfig.isOpenOn(tomorrow.getDayOfWeek())) {
            log.debug("Shop is closed on {}", tomorrow.getDayOfWeek());
            return List.of();
        }

        return getAvailableSlots(tomorrow, service, shopConfig.getOpeningTime());
    }

    /**
     * Core logic to calculate available slots
     */
    private List<LocalTime> getAvailableSlots(LocalDate date, Service service, LocalTime fromTime) {
        List<LocalTime> availableSlots = new ArrayList<>();

        LocalTime currentSlot = shopConfig.getOpeningTime();
        LocalTime closingTime = shopConfig.getClosingTime();
        Integer slotInterval = shopConfig.getSlotIntervalMinutes();
        Integer serviceDuration = service.getDurationMinutes();

        while (currentSlot.plusMinutes(serviceDuration).isBefore(closingTime) ||
               currentSlot.plusMinutes(serviceDuration).equals(closingTime)) {

            // Only consider slots after the minimum time
            if (currentSlot.isBefore(fromTime)) {
                currentSlot = currentSlot.plusMinutes(slotInterval);
                continue;
            }

            // Check if this slot has capacity
            if (isSlotAvailable(date, currentSlot, serviceDuration)) {
                availableSlots.add(currentSlot);
            }

            currentSlot = currentSlot.plusMinutes(slotInterval);
        }

        log.debug("Found {} available slots for {} on {} starting from {}",
                  availableSlots.size(), service.getName(), date, fromTime);
        return availableSlots;
    }

    /**
     * Check if a specific time slot has capacity
     * A slot is available if current bookings < number of barbers
     */
    private boolean isSlotAvailable(LocalDate date, LocalTime slotTime, Integer serviceDuration) {
        LocalTime serviceEndTime = slotTime.plusMinutes(serviceDuration);

        // Count bookings that overlap with this time window
        Long overlappingBookings = bookingRepository.countBookingsAtSlot(date, slotTime, serviceEndTime);

        boolean hasCapacity = overlappingBookings < shopConfig.getNumberOfBarbers();

        if (!hasCapacity) {
            log.debug("Slot {} on {} is full ({}/{} barbers booked)",
                      slotTime, date, overlappingBookings, shopConfig.getNumberOfBarbers());
        }

        return hasCapacity;
    }

    /**
     * Check if a specific slot is available for booking
     */
    public boolean validateSlotAvailability(LocalDate date, LocalTime time, Service service) {
        // Check if shop is open that day
        if (!shopConfig.isOpenOn(date.getDayOfWeek())) {
            log.warn("Attempted to book on closed day: {}", date.getDayOfWeek());
            return false;
        }

        // Check if time is within business hours
        LocalTime serviceEndTime = time.plusMinutes(service.getDurationMinutes());
        if (time.isBefore(shopConfig.getOpeningTime()) ||
            serviceEndTime.isAfter(shopConfig.getClosingTime())) {
            log.warn("Booking time {} is outside business hours", time);
            return false;
        }

        // Check if slot is in the past
        LocalDateTime bookingDateTime = LocalDateTime.of(date, time);
        if (bookingDateTime.isBefore(LocalDateTime.now())) {
            log.warn("Cannot book in the past: {}", bookingDateTime);
            return false;
        }

        // For today, enforce minimum advance booking
        if (date.equals(LocalDate.now())) {
            LocalTime minimumTime = LocalTime.now().plusHours(shopConfig.getMinimumAdvanceBookingHours());
            if (time.isBefore(minimumTime)) {
                log.warn("Booking time {} is less than {} hours from now",
                         time, shopConfig.getMinimumAdvanceBookingHours());
                return false;
            }
        }

        // Check capacity
        return isSlotAvailable(date, time, service.getDurationMinutes());
    }
}
