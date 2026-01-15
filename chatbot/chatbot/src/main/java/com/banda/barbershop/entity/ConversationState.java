package com.banda.barbershop.entity;

import com.banda.barbershop.enums.ConversationStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Conversation state model stored in Redis.
 * No longer a JPA entity - conversation state is ephemeral and managed via Redis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationState implements Serializable {

    private String phoneNumber;

    private ConversationStep currentStep;

    private String contextData;

    private LocalDateTime lastActivity;

    /**
     * Update last activity timestamp
     */
    public void touch() {
        this.lastActivity = LocalDateTime.now();
    }
}
