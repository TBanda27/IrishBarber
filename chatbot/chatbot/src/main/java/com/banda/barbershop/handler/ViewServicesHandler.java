package com.banda.barbershop.handler;

import com.banda.barbershop.config.BarberShopConfig;
import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.entity.Service;
import com.banda.barbershop.enums.ConversationStep;
import com.banda.barbershop.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewServicesHandler implements MessageHandler {

    private final ServiceRepository serviceRepository;
    private final BarberShopConfig shopConfig;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.VIEW_SERVICES;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        List<Service> services = serviceRepository.findByActiveOrderByDisplayOrder(true);

        if (services.isEmpty()) {
            return HandlerResponse.builder()
                .message("⚠️ No services available at the moment. Please try again later.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        String message = buildServicesMessage(services);

        // Check for menu command
        String userInput = request.getUserInput().toUpperCase().trim();
        if ("MENU".equals(userInput) || "0".equals(userInput)) {
            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.MAIN_MENU)
                .contextData("show_initial")
                .build();
        }

        // Check if user wants to book now
        if ("BOOK".equalsIgnoreCase(userInput) || "1".equals(userInput)) {
            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.SELECT_SERVICE)
                .contextData("show_initial")
                .build();
        }

        // Show services on initial entry or any input
        return HandlerResponse.builder()
            .message(message)
            .nextStep(ConversationStep.VIEW_SERVICES)
            .clearContext(true)
            .build();
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.VIEW_SERVICES;
    }

    private String buildServicesMessage(List<Service> services) {
        StringBuilder message = new StringBuilder();
        message.append("💈 *Our Services*\n\n");

        for (Service service : services) {
            message.append("✂️ *").append(service.getName()).append("*\n");
            if (service.getDescription() != null && !service.getDescription().isEmpty()) {
                message.append("   ").append(service.getDescription()).append("\n");
            }
            message.append("   💰 €").append(String.format("%.0f", service.getPrice()));
            message.append(" • ⏱️ ").append(service.getDurationMinutes()).append(" min\n\n");
        }

        message.append("📍 *").append(shopConfig.getName()).append("*\n");
        message.append(shopConfig.getAddress()).append("\n");
        if (shopConfig.getPhone() != null) {
            message.append("📞 ").append(shopConfig.getPhone()).append("\n");
        }
        message.append("\n");

        message.append("Ready to book?\n");
        message.append("1️⃣ Book Now\n");
        message.append("0️⃣ Main Menu");

        return message.toString();
    }
}
