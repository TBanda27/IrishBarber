package com.banda.barbershop.repository;

import com.banda.barbershop.entity.ConversationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationStateRepository extends JpaRepository<ConversationState, Long> {
    Optional<ConversationState> findByPhoneNumber(String phoneNumber);
}
