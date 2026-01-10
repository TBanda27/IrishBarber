package com.banda.barbershop.repository;

import com.banda.barbershop.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    /**
     * Find customers with birthdays today who haven't received message this year
     */
    @Query("SELECT c FROM Customer c WHERE c.birthdayMonth = :month " +
           "AND c.birthdayDay = :day " +
           "AND (c.lastBirthdayMessageSent IS NULL " +
           "OR YEAR(c.lastBirthdayMessageSent) < :currentYear)")
    List<Customer> findBirthdaysToday(
        @Param("month") int month,
        @Param("day") int day,
        @Param("currentYear") int currentYear
    );

    /**
     * Find customers by loyalty points (for rewards/promotions)
     */
    List<Customer> findByLoyaltyPointsGreaterThanEqual(Integer minPoints);

    /**
     * Find top customers by completed bookings
     */
    List<Customer> findTop10ByCompletedBookingsGreaterThanOrderByCompletedBookingsDesc(Integer minBookings);

    /**
     * Find customers who haven't visited recently (for re-engagement)
     */
    @Query("SELECT c FROM Customer c WHERE c.lastVisit < :cutoffDate " +
           "AND c.completedBookings > 0 ORDER BY c.lastVisit DESC")
    List<Customer> findInactiveCustomers(@Param("cutoffDate") LocalDate cutoffDate);
}
