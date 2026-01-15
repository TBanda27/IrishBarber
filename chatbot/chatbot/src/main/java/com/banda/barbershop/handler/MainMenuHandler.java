package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.enums.ConversationStep;
import org.springframework.stereotype.Component;

@Component
public class MainMenuHandler implements MessageHandler {

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.MAIN_MENU;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        String message = buildMainMenu();

        // If context is "show_initial" OR null/empty, show menu and ignore user input
        // This ensures first message after joining stream always shows menu
        String contextData = request.getContextData();
        if (contextData == null || contextData.isEmpty() || "show_initial".equals(contextData)) {
            return HandlerResponse.builder()
                .message(message)
                .nextStep(ConversationStep.MAIN_MENU)
                .contextData("ready")
                .build();
        }

        Integer choice = request.getParsedChoice();
        if (choice == null) {
            return HandlerResponse.builder()
                .message(message)
                .nextStep(ConversationStep.MAIN_MENU)
                .contextData("ready")
                .build();
        }

        ConversationStep nextStep = getNextStepFromChoice(choice);

        if (nextStep == ConversationStep.MAIN_MENU) {
            return HandlerResponse.builder()
                .message(message)
                .nextStep(nextStep)
                .contextData("ready")
                .build();
        }

        return HandlerResponse.builder()
            .message("")
            .nextStep(nextStep)
            .contextData("show_initial")
            .build();
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.MAIN_MENU;
    }

    private String buildMainMenu() {
        return """
            👋 *Welcome to Fade Factory Barbershop!*

            What would you like to do?

            1️⃣ View Services & Prices
            2️⃣ Book an Appointment
            3️⃣ View My Bookings
            4️⃣ Cancel a Booking
            5️⃣ FAQ / Help

            Reply with a number (1-5) or type MENU anytime
            """;
    }

    private ConversationStep getNextStepFromChoice(Integer choice) {
        return switch (choice) {
            case 1 -> ConversationStep.VIEW_SERVICES;
            case 2 -> ConversationStep.SELECT_SERVICE;
            case 3 -> ConversationStep.VIEW_MY_BOOKINGS;
            case 4 -> ConversationStep.CANCEL_BOOKING_INPUT;
            case 5 -> ConversationStep.FAQ;
            default -> ConversationStep.MAIN_MENU;
        };
    }
}
