package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.entity.Service;
import com.banda.barbershop.enums.ConversationStep;
import com.banda.barbershop.repository.ServiceRepository;
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
public class SelectServiceHandler implements MessageHandler {

    private final ServiceRepository serviceRepository;
    private final com.banda.barbershop.service.CustomerService customerService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.SELECT_SERVICE;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        List<Service> services = serviceRepository.findByActiveOrderByDisplayOrder(true);

        if (services.isEmpty()) {
            return HandlerResponse.builder()
                .message("⚠️ No services available at the moment. Please try again later.")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        // Show service menu on initial entry
        if ("show_initial".equals(request.getContextData())) {
            // Get personalized greeting if customer has history
            String greeting = customerService.getPersonalizedGreeting(request.getPhoneNumber());
            String menuMessage = greeting != null ?
                greeting + "\n" + buildServiceMenu(services) :
                buildServiceMenu(services);

            return HandlerResponse.builder()
                .message(menuMessage)
                .nextStep(ConversationStep.SELECT_SERVICE)
                .clearContext(true)
                .build();
        }

        // Check for menu command
        String userInput = request.getUserInput().toUpperCase().trim();
        if ("MENU".equals(userInput) || "0".equals(userInput)) {
            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        // Process user's service selection - invalid input re-displays the menu
        Integer choice = request.getParsedChoice();
        if (choice == null || choice < 1 || choice > services.size()) {
            return HandlerResponse.builder()
                .message(buildServiceMenu(services))
                .nextStep(ConversationStep.SELECT_SERVICE)
                .clearContext(true)
                .build();
        }

        Service selectedService = services.get(choice - 1);

        try {
            // Store selected service in context
            Map<String, Object> context = new HashMap<>();
            context.put("service_id", selectedService.getId());
            String contextJson = objectMapper.writeValueAsString(context);

            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.SELECT_BARBER)
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
        return ConversationStep.SELECT_SERVICE;
    }

    private String buildServiceMenu(List<Service> services) {
        StringBuilder menu = new StringBuilder();
        menu.append("🪒 *Select Your Service*\n\n");

        for (int i = 0; i < services.size(); i++) {
            Service service = services.get(i);
            menu.append(String.format("%d️⃣ %s - €%.0f (%d min)\n",
                i + 1,
                service.getName(),
                service.getPrice(),
                service.getDurationMinutes()
            ));
        }

        menu.append("\nReply with a number to continue");
        menu.append("\n0️⃣ Main Menu");

        return menu.toString();
    }
}
