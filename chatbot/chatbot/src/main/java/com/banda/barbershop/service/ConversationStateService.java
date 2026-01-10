package com.banda.barbershop.service;

import com.banda.barbershop.entity.ConversationState;
import com.banda.barbershop.enums.ConversationStep;
import com.banda.barbershop.repository.ConversationStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationStateService {

    private final ConversationStateRepository repository;

    @Transactional
    public ConversationState getOrCreate(String phoneNumber) {
        return repository.findByPhoneNumber(phoneNumber)
            .orElseGet(() -> createNewConversation(phoneNumber));
    }

    @Transactional
    public void updateStep(String phoneNumber, ConversationStep step) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setCurrentStep(step);
        repository.save(state);
    }

    @Transactional
    public void updateContext(String phoneNumber, String contextData) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setContextData(contextData);
        repository.save(state);
    }

    @Transactional
    public void updateStepAndContext(String phoneNumber, ConversationStep step, String contextData) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setCurrentStep(step);
        state.setContextData(contextData);
        repository.save(state);
    }

    @Transactional
    public void clearContext(String phoneNumber) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setContextData(null);
        repository.save(state);
    }

    @Transactional
    public void resetToMainMenu(String phoneNumber) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setCurrentStep(ConversationStep.MAIN_MENU);
        state.setContextData("show_initial");
        repository.save(state);
    }

    private ConversationState createNewConversation(String phoneNumber) {
        ConversationState newState = ConversationState.builder()
            .phoneNumber(phoneNumber)
            .currentStep(ConversationStep.MAIN_MENU)
            .build();
        return repository.save(newState);
    }
}
