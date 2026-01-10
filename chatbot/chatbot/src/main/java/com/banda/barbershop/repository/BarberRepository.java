package com.banda.barbershop.repository;

import com.banda.barbershop.entity.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarberRepository extends JpaRepository<Barber, Long> {

    /**
     * Find active barbers ordered by display order
     */
    List<Barber> findByActiveOrderByDisplayOrder(boolean active);

    /**
     * Find barber by phone number
     */
    Optional<Barber> findByPhoneNumber(String phoneNumber);

    /**
     * Find all active barbers ordered by display order then name
     */
    @Query("SELECT b FROM Barber b WHERE b.active = true ORDER BY b.displayOrder, b.name")
    List<Barber> findAllActiveBarbers();
}
