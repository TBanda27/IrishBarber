package com.banda.barbershop.service;

import com.banda.barbershop.config.LoyaltyConfig;
import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.entity.Customer;
import com.banda.barbershop.entity.Service;
import com.banda.barbershop.repository.BarberRepository;
import com.banda.barbershop.repository.BookingRepository;
import com.banda.barbershop.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final BarberRepository barberRepository;
    private final LoyaltyConfig loyaltyConfig;

    /**
     * Get or create customer profile
     */
    @Transactional
    public Customer getOrCreateCustomer(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
            .orElseGet(() -> createNewCustomer(phoneNumber));
    }

    /**
     * Update customer profile after booking is created
     */
    @Transactional
    public void recordBooking(String phoneNumber, Service service, Long barberId) {
        Customer customer = getOrCreateCustomer(phoneNumber);

        customer.setTotalBookings(customer.getTotalBookings() + 1);

        // Track preferred service (most booked service)
        updatePreferredService(customer, service);

        // Track preferred barber (most booked barber)
        updatePreferredBarber(customer, barberId);

        // Award points for booking (if first booking, give bonus)
        if (customer.getTotalBookings() == 1 && loyaltyConfig.isEnabled()) {
            int points = loyaltyConfig.getPointsPerBooking() + loyaltyConfig.getBonusPointsForFirstBooking();
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
            customer.setLifetimeLoyaltyPoints(customer.getLifetimeLoyaltyPoints() + points);
            log.info("First booking bonus! Customer {} earned {} points", phoneNumber, points);
        } else if (loyaltyConfig.isEnabled()) {
            int points = loyaltyConfig.getPointsPerBooking();
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
            customer.setLifetimeLoyaltyPoints(customer.getLifetimeLoyaltyPoints() + points);
        }

        // Set first visit date
        if (customer.getFirstVisit() == null) {
            customer.setFirstVisit(LocalDate.now());
        }

        customerRepository.save(customer);
        log.info("Recorded booking for customer {}: total bookings = {}",
                 phoneNumber, customer.getTotalBookings());
    }

    /**
     * Update customer profile after booking is completed
     */
    @Transactional
    public void recordCompletedBooking(String phoneNumber) {
        Customer customer = getOrCreateCustomer(phoneNumber);
        customer.setCompletedBookings(customer.getCompletedBookings() + 1);
        customer.setLastVisit(LocalDate.now());
        customerRepository.save(customer);

        log.info("Customer {} completed booking #{}", phoneNumber, customer.getCompletedBookings());
    }

    /**
     * Update customer profile after booking is cancelled
     */
    @Transactional
    public void recordCancelledBooking(String phoneNumber) {
        Customer customer = getOrCreateCustomer(phoneNumber);
        customer.setCancelledBookings(customer.getCancelledBookings() + 1);
        customerRepository.save(customer);
    }

    /**
     * Update customer profile after no-show
     */
    @Transactional
    public void recordNoShow(String phoneNumber) {
        Customer customer = getOrCreateCustomer(phoneNumber);
        customer.setNoShowBookings(customer.getNoShowBookings() + 1);
        customerRepository.save(customer);
    }

    /**
     * Get personalized greeting/suggestion for customer
     */
    public String getPersonalizedGreeting(String phoneNumber) {
        Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(phoneNumber);

        if (customerOpt.isEmpty()) {
            return null; // New customer, no personalization
        }

        Customer customer = customerOpt.get();

        // Check if it's their birthday
        if (customer.isBirthdayToday() &&
            !customer.birthdayMessageSentThisYear() &&
            loyaltyConfig.getBirthday().isEnabled()) {
            return buildBirthdayGreeting(customer);
        }

        // Check for milestone
        if (loyaltyConfig.isEnabled() && loyaltyConfig.isMilestone(customer.getCompletedBookings())) {
            return buildMilestoneGreeting(customer);
        }

        // Suggest usual service
        if (customer.getPreferredService() != null && customer.getPreferredServiceCount() >= 3) {
            return buildPreferredServiceSuggestion(customer);
        }

        // Welcome back returning customer
        if (customer.getTotalBookings() > 0) {
            return buildWelcomeBackMessage(customer);
        }

        return null;
    }

    /**
     * Get customer's preferred service for quick booking
     */
    public Optional<Service> getPreferredService(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
            .map(Customer::getPreferredService)
            .filter(service -> service != null);
    }

    /**
     * Check if customer qualifies for loyalty milestone message
     */
    public Optional<String> checkLoyaltyMilestone(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
            .filter(c -> loyaltyConfig.isMilestone(c.getCompletedBookings()))
            .map(c -> loyaltyConfig.getMilestoneMessage(c.getCompletedBookings()));
    }

    /**
     * Send birthday messages to customers
     */
    @Transactional
    public List<Customer> getTodaysBirthdays() {
        LocalDate today = LocalDate.now();
        return customerRepository.findBirthdaysToday(
            today.getMonthValue(),
            today.getDayOfMonth(),
            today.getYear()
        );
    }

    /**
     * Mark birthday message as sent
     */
    @Transactional
    public void markBirthdayMessageSent(String phoneNumber) {
        Customer customer = getOrCreateCustomer(phoneNumber);
        customer.setLastBirthdayMessageSent(LocalDate.now());
        customerRepository.save(customer);
    }

    // ==================== Private Helper Methods ====================

    private Customer createNewCustomer(String phoneNumber) {
        Customer customer = Customer.builder()
            .phoneNumber(phoneNumber)
            .totalBookings(0)
            .completedBookings(0)
            .cancelledBookings(0)
            .noShowBookings(0)
            .loyaltyPoints(0)
            .lifetimeLoyaltyPoints(0)
            .build();

        Customer saved = customerRepository.save(customer);
        log.info("Created new customer profile for {}", phoneNumber);
        return saved;
    }

    private void updatePreferredService(Customer customer, Service service) {
        // Count how many times customer booked each service
        List<Booking> completedBookings = bookingRepository.findByCustomerPhoneAndStatusIn(
            customer.getPhoneNumber(),
            List.of(Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.COMPLETED)
        );

        Map<Long, Integer> serviceCounts = new HashMap<>();
        for (Booking booking : completedBookings) {
            Long serviceId = booking.getService().getId();
            serviceCounts.put(serviceId, serviceCounts.getOrDefault(serviceId, 0) + 1);
        }

        // Find most booked service
        serviceCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(entry -> {
                if (entry.getKey().equals(service.getId())) {
                    customer.setPreferredService(service);
                    customer.setPreferredServiceCount(entry.getValue());
                }
            });
    }

    private void updatePreferredBarber(Customer customer, Long barberId) {
        // Count how many times customer booked each barber
        List<Booking> completedBookings = bookingRepository.findByCustomerPhoneAndStatusIn(
            customer.getPhoneNumber(),
            List.of(Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.COMPLETED)
        );

        Map<Long, Integer> barberCounts = new HashMap<>();
        for (Booking booking : completedBookings) {
            // Only count bookings that have a barber assigned
            if (booking.getBarber() != null) {
                Long bookingBarberId = booking.getBarber().getId();
                barberCounts.put(bookingBarberId, barberCounts.getOrDefault(bookingBarberId, 0) + 1);
            }
        }

        // Find most booked barber
        barberCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(entry -> {
                if (entry.getKey().equals(barberId)) {
                    barberRepository.findById(barberId).ifPresent(barber -> {
                        customer.setPreferredBarber(barber);
                        customer.setPreferredBarberCount(entry.getValue());
                        log.debug("Updated preferred barber for customer {} to {} ({} bookings)",
                                 customer.getPhoneNumber(), barber.getName(), entry.getValue());
                    });
                }
            });
    }

    private String buildBirthdayGreeting(Customer customer) {
        if (!loyaltyConfig.getBirthday().isEnabled()) {
            return null;
        }

        int bonusPoints = loyaltyConfig.getBirthday().getBonusPoints();
        String discountCode = loyaltyConfig.getBirthday().getDiscountCode();
        int discount = loyaltyConfig.getBirthday().getDiscountPercent();

        return String.format("""
            🎉 *HAPPY BIRTHDAY* 🎂

            We're celebrating YOU today!

            🎁 Birthday Gift:
            • %d Loyalty Points
            • %d%% OFF your next booking
            • Use code: *%s*

            Book today to redeem your birthday reward! 🎈
            """,
            bonusPoints,
            discount,
            discountCode
        );
    }

    private String buildMilestoneGreeting(Customer customer) {
        String milestoneMsg = loyaltyConfig.getMilestoneMessage(customer.getCompletedBookings());

        return String.format("""
            %s

            💎 You have %d loyalty points
            📊 Total visits: %d

            Thank you for being an amazing customer!
            """,
            milestoneMsg,
            customer.getLoyaltyPoints(),
            customer.getCompletedBookings()
        );
    }

    private String buildPreferredServiceSuggestion(Customer customer) {
        return String.format("""
            Welcome back! 👋

            🪒 Quick book your usual? *%s*

            Or browse all services below:
            """,
            customer.getPreferredService().getName()
        );
    }

    private String buildWelcomeBackMessage(Customer customer) {
        int visits = customer.getCompletedBookings();

        if (visits == 0) {
            return null; // First booking not yet completed
        }

        if (visits == 1) {
            return "Welcome back! 👋 Great to see you again!";
        }

        return String.format("""
            Welcome back! 👋

            Visit #%d • %d Loyalty Points 💎
            """,
            visits + 1,
            customer.getLoyaltyPoints()
        );
    }
}
