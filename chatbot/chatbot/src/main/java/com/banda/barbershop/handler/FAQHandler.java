package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.enums.ConversationStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FAQHandler implements MessageHandler {

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.FAQ;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        String userInput = request.getUserInput().toUpperCase().trim();

        // Check for menu command
        if ("MENU".equals(userInput) || "0".equals(userInput)) {
            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        // Show FAQ categories on initial entry
        if ("show_initial".equals(request.getContextData())) {
            return HandlerResponse.builder()
                .message(buildFAQMenu())
                .nextStep(ConversationStep.FAQ)
                .clearContext(true)
                .build();
        }

        // Handle FAQ category selection
        Integer choice = request.getParsedChoice();
        if (choice != null && choice >= 1 && choice <= 5) {
            String answer = getFAQAnswer(choice);
            return HandlerResponse.builder()
                .message(answer + "\n\n" + buildFAQMenu())
                .nextStep(ConversationStep.FAQ)
                .clearContext(true)
                .build();
        }

        // Invalid input
        return HandlerResponse.builder()
            .message(buildFAQMenu())
            .nextStep(ConversationStep.FAQ)
            .clearContext(true)
            .build();
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.FAQ;
    }

    private String buildFAQMenu() {
        return """
            ❓ *Frequently Asked Questions*

            1️⃣ What are your opening hours?
            2️⃣ Where are you located?
            3️⃣ Do I need to book in advance?
            4️⃣ What payment methods do you accept?
            5️⃣ Can I cancel or reschedule?

            Reply with a number for more info
            0️⃣ Main Menu
            """;
    }

    private String getFAQAnswer(int choice) {
        return switch (choice) {
            case 1 -> """
                ⏰ *Opening Hours*

                Monday - Saturday: 9:00 AM - 7:00 PM
                Sunday: Closed

                We recommend booking in advance to avoid waiting!
                """;

            case 2 -> """
                📍 *Location*

                Fade Factory Barbershop
                123 Main St, Dublin
                Ireland

                🚗 Free parking available
                🚌 Bus routes: 4, 7, 16
                🚇 Nearest station: O'Connell Street

                Easy to find, right in the city center!
                """;

            case 3 -> """
                📅 *Booking Policy*

                ✅ We recommend booking in advance
                ✅ Bookings available for today and tomorrow
                ✅ Minimum 2 hours advance notice required
                ✅ Walk-ins welcome if we have availability

                You can book through this WhatsApp chat anytime!
                """;

            case 4 -> """
                💳 *Payment Methods*

                We accept:
                ✅ Cash (EUR)
                ✅ Credit/Debit Cards (Visa, Mastercard)
                ✅ Revolut
                ✅ Apple Pay
                ✅ Google Pay

                Payment is taken after your service.
                """;

            case 5 -> """
                🔄 *Cancellation & Rescheduling*

                ✅ Free cancellation anytime via WhatsApp
                ✅ Just send us your booking code
                ✅ Want to reschedule? Cancel and book again
                ✅ No cancellation fees

                We understand plans change - just let us know!
                """;

            default -> "Please select a valid option (1-5)";
        };
    }
}
