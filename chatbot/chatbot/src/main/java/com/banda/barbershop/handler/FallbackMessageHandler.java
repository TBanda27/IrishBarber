package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.enums.ConversationStep;
import org.springframework.stereotype.Component;

@Component
public class FallbackMessageHandler implements MessageHandler {

    @Override
    public boolean canHandle(ConversationStep step) {
        return false;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        String userInput = request.getUserInput().toUpperCase().trim();

        // Check for menu commands
        if ("MENU".equals(userInput) || "0".equals(userInput) || "MAIN".equals(userInput)) {
            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        // Default fallback message
        String message = """
            ⚠️ I didn't understand that.

            Reply 0 or MENU to return to main menu
            """;

        return HandlerResponse.builder()
            .message(message)
            .nextStep(request.getCurrentStep())
            .contextData(request.getContextData())
            .build();
    }

    @Override
    public ConversationStep getHandledStep() {
        return null;
    }
}
