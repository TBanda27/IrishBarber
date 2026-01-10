package com.banda.barbershop.entity;

import com.banda.barbershop.enums.ConversationStep;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConversationStep currentStep;

    @Column(columnDefinition = "TEXT")
    private String contextData;

    @Column(nullable = false)
    private LocalDateTime lastActivity;

    @PrePersist
    @PreUpdate
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}
