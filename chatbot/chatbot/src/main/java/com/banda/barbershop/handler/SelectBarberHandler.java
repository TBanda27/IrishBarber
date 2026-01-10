package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.entity.Barber;
import com.banda.barbershop.entity.Customer;
import com.banda.barbershop.enums.ConversationStep;
import com.banda.barbershop.repository.BarberRepository;
import com.banda.barbershop.service.CustomerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SelectBarberHandler implements MessageHandler {

    private final BarberRepository barberRepository;
    private final CustomerService customerService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.SELECT_BARBER;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        List<Barber> barbers = barberRepository.findByActiveOrderByDisplayOrder(true);

        if (barbers.isEmpty()) {
            return HandlerResponse.builder()
                .message("⚠️ No barbers available at the moment. Please try again later.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        // Show barber menu on initial entry (when context has service_id but hasn't shown barbers yet)
        try {
            Map<String, Object> context = parseContext(request.getContextData());

            // Check if we need to show the initial menu
            if (context.containsKey("service_id") && !context.containsKey("barbers_shown")) {
                // Get customer's preferred barber for personalized menu
                Customer customer = customerService.getOrCreateCustomer(request.getPhoneNumber());
                Barber preferredBarber = customer.getPreferredBarber();

                String menuMessage = buildBarberMenu(barbers, preferredBarber);

                // Mark that we've shown the barbers menu
                context.put("barbers_shown", true);
                String contextJson = objectMapper.writeValueAsString(context);

                return HandlerResponse.builder()
                    .message(menuMessage)
                    .nextStep(ConversationStep.SELECT_BARBER)
                    .contextData(contextJson)
                    .build();
            }
        } catch (Exception e) {
            log.error("Error parsing context in SelectBarberHandler", e);
        }

        // Handle menu command
        String userInput = request.getUserInput().toUpperCase().trim();
        if ("MENU".equals(userInput) || "0".equals(userInput)) {
            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.MAIN_MENU)
                .contextData("show_initial")
                .build();
        }

        // Process barber selection
        Integer choice = request.getParsedChoice();
        if (choice == null || choice < 1 || choice > barbers.size()) {
            Customer customer = customerService.getOrCreateCustomer(request.getPhoneNumber());
            Barber preferredBarber = customer.getPreferredBarber();

            return HandlerResponse.builder()
                .message(buildBarberMenu(barbers, preferredBarber) +
                        "\n\n⚠️ Please enter a valid number (1-" + barbers.size() + ")")
                .nextStep(ConversationStep.SELECT_BARBER)
                .contextData(request.getContextData())
                .build();
        }

        Barber selectedBarber = barbers.get(choice - 1);

        try {
            // Parse existing context and add barber_id
            Map<String, Object> context = parseContext(request.getContextData());
            context.put("barber_id", selectedBarber.getId());
            context.remove("barbers_shown"); // Clean up
            String contextJson = objectMapper.writeValueAsString(context);

            log.info("Customer {} selected barber: {}", request.getPhoneNumber(), selectedBarber.getName());

            return HandlerResponse.builder()
                .message("") // ViewSlotsHandler will show slots
                .nextStep(ConversationStep.VIEW_TODAY_SLOTS)
                .contextData(contextJson)
                .build();

        } catch (Exception e) {
            log.error("Failed to serialize context", e);
            return HandlerResponse.builder()
                .message("⚠️ Something went wrong. Please try again.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.SELECT_BARBER;
    }

    private String buildBarberMenu(List<Barber> barbers, Barber preferredBarber) {
        StringBuilder menu = new StringBuilder();
        menu.append("👨‍🦲 *Select Your Barber*\n\n");

        for (int i = 0; i < barbers.size(); i++) {
            Barber barber = barbers.get(i);

            // Number and name
            menu.append(String.format("%d️⃣ %s", i + 1, barber.getName()));

            // Show rating if available
            if (barber.getRating() != null && barber.getRating() > 0) {
                menu.append(String.format(" ⭐ %.1f", barber.getRating()));
            }

            // Mark preferred barber
            if (preferredBarber != null && barber.getId().equals(preferredBarber.getId())) {
                menu.append(" (Your Usual)");
            }

            menu.append("\n");

            // Show bio if available
            if (barber.getBio() != null && !barber.getBio().isEmpty()) {
                menu.append("   ").append(barber.getBio()).append("\n");
            }

            // Add spacing between barbers
            if (i < barbers.size() - 1) {
                menu.append("\n");
            }
        }

        menu.append("\nReply with a number to continue");
        menu.append("\n0️⃣ Main Menu");

        return menu.toString();
    }

    private Map<String, Object> parseContext(String contextData) throws Exception {
        if (contextData == null || contextData.isEmpty() || "show_initial".equals(contextData)) {
            return new HashMap<>();
        }
        return objectMapper.readValue(contextData, new TypeReference<>() {});
    }
}
