package com.banda.barbershop.service;

import com.banda.barbershop.dto.*;
import com.banda.barbershop.entity.Barber;
import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.entity.Booking.BookingStatus;
import com.banda.barbershop.repository.BarberRepository;
import com.banda.barbershop.repository.BookingRepository;
import com.banda.barbershop.repository.CustomerRepository;
import com.banda.barbershop.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final BarberRepository barberRepository;

    /**
     * Get dashboard statistics
     */
    public DashboardStatsDTO getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minus(7, ChronoUnit.DAYS);
        LocalDate monthAgo = today.minus(30, ChronoUnit.DAYS);

        // Today's bookings
        List<Booking> todayBookings = bookingRepository.findByBookingDateAndStatus(today, BookingStatus.CONFIRMED);
        todayBookings.addAll(bookingRepository.findByBookingDateAndStatus(today, BookingStatus.COMPLETED));
        todayBookings.addAll(bookingRepository.findByBookingDateAndStatus(today, BookingStatus.CANCELLED));

        int todayConfirmed = (int) todayBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
            .count();
        int todayCompleted = (int) todayBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .count();
        int todayCancelled = (int) todayBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
            .count();
        int todayNoShows = (int) todayBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.NO_SHOW)
            .count();

        double todayRevenue = todayBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .mapToDouble(b -> b.getService().getPrice().doubleValue())
            .sum();

        // Get all bookings for week/month calculations
        List<Booking> allBookings = bookingRepository.findAll();

        int weekBookings = (int) allBookings.stream()
            .filter(b -> !b.getBookingDate().isBefore(weekAgo))
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
            .count();

        double weekRevenue = allBookings.stream()
            .filter(b -> !b.getBookingDate().isBefore(weekAgo))
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .mapToDouble(b -> b.getService().getPrice().doubleValue())
            .sum();

        int monthBookings = (int) allBookings.stream()
            .filter(b -> !b.getBookingDate().isBefore(monthAgo))
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
            .count();

        double monthRevenue = allBookings.stream()
            .filter(b -> !b.getBookingDate().isBefore(monthAgo))
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .mapToDouble(b -> b.getService().getPrice().doubleValue())
            .sum();

        // Customer stats
        long totalCustomers = customerRepository.count();
        long activeCustomers = customerRepository.findAll().stream()
            .filter(c -> c.getLastVisit() != null)
            .filter(c -> !c.getLastVisit().isBefore(monthAgo))
            .count();

        double avgBookingValue = allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .mapToDouble(b -> b.getService().getPrice().doubleValue())
            .average()
            .orElse(0.0);

        // Popular services
        Map<String, Long> serviceCount = allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .collect(Collectors.groupingBy(
                b -> b.getService().getName(),
                Collectors.counting()
            ));

        Map<String, Double> serviceRevenue = allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .collect(Collectors.groupingBy(
                b -> b.getService().getName(),
                Collectors.summingDouble(b -> b.getService().getPrice().doubleValue())
            ));

        List<DashboardStatsDTO.ServiceStatsDTO> popularServices = serviceCount.entrySet().stream()
            .map(entry -> DashboardStatsDTO.ServiceStatsDTO.builder()
                .serviceName(entry.getKey())
                .bookingCount(entry.getValue())
                .totalRevenue(serviceRevenue.getOrDefault(entry.getKey(), 0.0))
                .build())
            .sorted((a, b) -> Long.compare(b.getBookingCount(), a.getBookingCount()))
            .limit(5)
            .collect(Collectors.toList());

        // Upcoming bookings
        int upcomingToday = (int) allBookings.stream()
            .filter(b -> b.getBookingDate().equals(today))
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .count();

        int upcomingTomorrow = (int) allBookings.stream()
            .filter(b -> b.getBookingDate().equals(today.plusDays(1)))
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .count();

        return DashboardStatsDTO.builder()
            .todayBookings(todayConfirmed)
            .todayCompleted(todayCompleted)
            .todayCancelled(todayCancelled)
            .todayNoShows(todayNoShows)
            .todayRevenue(todayRevenue)
            .weekBookings(weekBookings)
            .weekRevenue(weekRevenue)
            .monthBookings(monthBookings)
            .monthRevenue(monthRevenue)
            .totalCustomers((int) totalCustomers)
            .activeCustomers((int) activeCustomers)
            .averageBookingValue(avgBookingValue)
            .popularServices(popularServices)
            .upcomingToday(upcomingToday)
            .upcomingTomorrow(upcomingTomorrow)
            .build();
    }

    /**
     * Get bookings for a specific date
     */
    public List<BookingDTO> getBookingsByDate(LocalDate date) {
        List<Booking> bookings = bookingRepository.findAll().stream()
            .filter(b -> b.getBookingDate().equals(date))
            .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
            .toList();

        return bookings.stream()
            .map(BookingDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Mark booking as completed
     */
    @Transactional
    public boolean markAsCompleted(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
            .map(booking -> {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);
                log.info("Marked booking {} as completed", bookingCode);
                return true;
            })
            .orElse(false);
    }

    /**
     * Mark booking as no-show
     */
    @Transactional
    public boolean markAsNoShow(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
            .map(booking -> {
                booking.setStatus(BookingStatus.NO_SHOW);
                bookingRepository.save(booking);
                log.info("Marked booking {} as no-show", bookingCode);
                return true;
            })
            .orElse(false);
    }

    /**
     * Get all customers
     */
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
            .sorted((a, b) -> {
                if (a.getLastVisit() == null) return 1;
                if (b.getLastVisit() == null) return -1;
                return b.getLastVisit().compareTo(a.getLastVisit());
            })
            .map(CustomerDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get top customers by loyalty points
     */
    public List<CustomerDTO> getTopCustomers(int limit) {
        return customerRepository.findAll().stream()
            .sorted((a, b) -> Integer.compare(b.getLoyaltyPoints(), a.getLoyaltyPoints()))
            .limit(limit)
            .map(CustomerDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get all services
     */
    public List<ServiceDTO> getAllServices() {
        return serviceRepository.findAll().stream()
            .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
            .map(ServiceDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get service by ID
     */
    public ServiceDTO getServiceById(Long id) {
        return serviceRepository.findById(id)
            .map(ServiceDTO::fromEntity)
            .orElse(null);
    }

    /**
     * Create new service
     */
    @Transactional
    public ServiceDTO createService(ServiceDTO serviceDTO) {
        com.banda.barbershop.entity.Service service = serviceDTO.toEntity();
        com.banda.barbershop.entity.Service saved = serviceRepository.save(service);
        log.info("Created new service: {}", saved.getName());
        return ServiceDTO.fromEntity(saved);
    }

    /**
     * Update existing service
     */
    @Transactional
    public ServiceDTO updateService(Long id, ServiceDTO serviceDTO) {
        return serviceRepository.findById(id)
            .map(existing -> {
                existing.setName(serviceDTO.getName());
                existing.setDescription(serviceDTO.getDescription());
                existing.setPrice(serviceDTO.getPrice());
                existing.setDurationMinutes(serviceDTO.getDurationMinutes());
                existing.setActive(serviceDTO.getActive());
                existing.setDisplayOrder(serviceDTO.getDisplayOrder());
                com.banda.barbershop.entity.Service updated = serviceRepository.save(existing);
                log.info("Updated service: {}", updated.getName());
                return ServiceDTO.fromEntity(updated);
            })
            .orElse(null);
    }

    /**
     * Delete service
     */
    @Transactional
    public boolean deleteService(Long id) {
        if (serviceRepository.existsById(id)) {
            serviceRepository.deleteById(id);
            log.info("Deleted service with ID: {}", id);
            return true;
        }
        return false;
    }

    // ==================== Barber Management ====================

    /**
     * Get all barbers
     */
    public List<BarberDTO> getAllBarbers() {
        return barberRepository.findAll().stream()
            .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
            .map(BarberDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get barber by ID
     */
    public BarberDTO getBarberById(Long id) {
        return barberRepository.findById(id)
            .map(BarberDTO::fromEntity)
            .orElse(null);
    }

    /**
     * Create new barber
     */
    @Transactional
    public BarberDTO createBarber(BarberDTO barberDTO) {
        Barber barber = barberDTO.toEntity();
        Barber saved = barberRepository.save(barber);
        log.info("Created new barber: {}", saved.getName());
        return BarberDTO.fromEntity(saved);
    }

    /**
     * Update existing barber
     */
    @Transactional
    public BarberDTO updateBarber(Long id, BarberDTO barberDTO) {
        return barberRepository.findById(id)
            .map(existing -> {
                existing.setName(barberDTO.getName());
                existing.setPhoneNumber(barberDTO.getPhoneNumber());
                existing.setActive(barberDTO.getActive());
                existing.setDisplayOrder(barberDTO.getDisplayOrder());
                existing.setBio(barberDTO.getBio());
                Barber updated = barberRepository.save(existing);
                log.info("Updated barber: {}", updated.getName());
                return BarberDTO.fromEntity(updated);
            })
            .orElse(null);
    }

    /**
     * Delete barber (soft delete - set active=false)
     */
    @Transactional
    public boolean deleteBarber(Long id) {
        return barberRepository.findById(id)
            .map(barber -> {
                barber.setActive(false);
                barberRepository.save(barber);
                log.info("Deactivated barber: {}", barber.getName());
                return true;
            })
            .orElse(false);
    }

    /**
     * Get barber schedule for specific date
     */
    public BarberScheduleDTO getBarberSchedule(Long barberId, LocalDate date) {
        Barber barber = barberRepository.findById(barberId).orElse(null);
        if (barber == null) {
            return null;
        }

        List<Booking> bookings = bookingRepository.findByBarberAndDate(barberId, date);

        List<BarberScheduleDTO.BookingSlot> slots = bookings.stream()
            .map(booking -> BarberScheduleDTO.BookingSlot.builder()
                .bookingCode(booking.getBookingCode())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .customerPhone(booking.getCustomerPhone())
                .serviceName(booking.getService().getName())
                .status(booking.getStatus().name())
                .build())
            .collect(Collectors.toList());

        // Calculate utilization (assuming 10 working hours = 600 minutes)
        int totalMinutesBooked = bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
            .mapToInt(b -> b.getService().getDurationMinutes())
            .sum();
        double utilization = (totalMinutesBooked / 600.0) * 100.0;

        return BarberScheduleDTO.builder()
            .barberId(barber.getId())
            .barberName(barber.getName())
            .date(date)
            .bookings(slots)
            .totalBookings(bookings.size())
            .utilization(utilization)
            .build();
    }

    /**
     * Get barber statistics (performance metrics)
     */
    public List<BarberStatsDTO> getBarberStats() {
        List<Barber> barbers = barberRepository.findAll();
        LocalDate monthAgo = LocalDate.now().minus(30, ChronoUnit.DAYS);

        return barbers.stream()
            .map(barber -> {
                // Get recent bookings (last 30 days)
                List<Booking> recentBookings = bookingRepository.findByBarberIdOrderByBookingDateDesc(barber.getId())
                    .stream()
                    .filter(b -> !b.getBookingDate().isBefore(monthAgo))
                    .toList();

                int recentCount = recentBookings.size();
                int completedCount = (int) recentBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .count();

                double completionRate = barber.getTotalBookings() > 0
                    ? (barber.getCompletedBookings().doubleValue() / barber.getTotalBookings().doubleValue()) * 100.0
                    : 0.0;

                return BarberStatsDTO.builder()
                    .barberId(barber.getId())
                    .barberName(barber.getName())
                    .totalBookings(barber.getTotalBookings())
                    .completedBookings(barber.getCompletedBookings())
                    .recentBookings(recentCount)
                    .rating(barber.getRating())
                    .completionRate(completionRate)
                    .build();
            })
            .sorted((a, b) -> Integer.compare(b.getTotalBookings(), a.getTotalBookings()))
            .collect(Collectors.toList());
    }
}
