package com.banda.barbershop.repository;

import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.entity.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByCustomerPhoneAndStatusOrderByBookingDateDesc(
        String customerPhone,
        BookingStatus status
    );

    @Query("SELECT b FROM Booking b WHERE b.customerPhone = :phone " +
           "AND b.status IN :statuses ORDER BY b.bookingDate DESC, b.startTime DESC")
    List<Booking> findByCustomerPhoneAndStatusIn(
        @Param("phone") String customerPhone,
        @Param("statuses") List<BookingStatus> statuses
    );

    @Query("SELECT b FROM Booking b WHERE b.bookingDate = :date " +
           "AND b.status = 'CONFIRMED' " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findOverlappingBookings(
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate = :date " +
           "AND b.status = 'CONFIRMED' " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    Long countBookingsAtSlot(
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    List<Booking> findByBookingDateAndStatus(LocalDate date, BookingStatus status);

    /**
     * Count bookings for specific barber at time slot (overlapping)
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.barber.id = :barberId " +
           "AND b.bookingDate = :date AND b.status = 'CONFIRMED' " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    Long countBarberBookingsAtSlot(
        @Param("barberId") Long barberId,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    /**
     * Get bookings for barber on specific date (for schedule view)
     */
    @Query("SELECT b FROM Booking b WHERE b.barber.id = :barberId " +
           "AND b.bookingDate = :date AND b.status IN ('CONFIRMED', 'COMPLETED') " +
           "ORDER BY b.startTime")
    List<Booking> findByBarberAndDate(
        @Param("barberId") Long barberId,
        @Param("date") LocalDate date
    );

    /**
     * Get all bookings for barber
     */
    List<Booking> findByBarberIdOrderByBookingDateDesc(Long barberId);

    /**
     * Find confirmed bookings that have passed their end time (ready for completion)
     * Used by scheduler to auto-complete bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND (b.bookingDate < :today OR (b.bookingDate = :today AND b.endTime <= :currentTime))")
    List<Booking> findBookingsReadyForCompletion(
        @Param("today") LocalDate today,
        @Param("currentTime") LocalTime currentTime
    );

    /**
     * Find confirmed bookings from past days that were never completed (potential no-shows)
     * Grace period: bookings from before today that are still CONFIRMED
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.bookingDate < :today")
    List<Booking> findPotentialNoShows(@Param("today") LocalDate today);
}
