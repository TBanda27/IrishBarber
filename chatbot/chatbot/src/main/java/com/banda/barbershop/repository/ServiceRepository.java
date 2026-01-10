package com.banda.barbershop.repository;

import com.banda.barbershop.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByActiveOrderByDisplayOrder(boolean active);
}
