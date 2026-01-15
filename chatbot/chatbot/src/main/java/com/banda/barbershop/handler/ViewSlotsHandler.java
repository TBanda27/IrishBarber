package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.entity.Barber;
import com.banda.barbershop.entity.Service;
import com.banda.barbershop.enums.ConversationStep;
import com.banda.barbershop.repository.BarberRepository;
import com.banda.barbershop.repository.ServiceRepository;
import com.banda.barbershop.service.AvailabilityService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewSlotsHandler implements MessageHandler {

    private final AvailabilityService availabilityService;
    private final ServiceRepository serviceRepository;
    private final BarberRepository barberRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.VIEW_TODAY_SLOTS ||
               step == ConversationStep.VIEW_TOMORROW_SLOTS;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        try {
            Map<String, Object> context = parseContext(request.getContextData());
            Long serviceId = ((Number) context.get("service_id")).longValue();
            Long barberId = ((Number) context.get("barber_id")).longValue();

            Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalStateException("Service not found"));

            Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new IllegalStateException("Barber not found"));

            boolean isToday = request.getCurrentStep() == ConversationStep.VIEW_TODAY_SLOTS;

            LocalDate targetDate = isToday ? LocalDate.now() : LocalDate.now().plusDays(1);
            List<LocalTime> availableSlots = availabilityService.getAvailableSlotsForBarber(
                service, barberId, targetDate);

            // If no slots available for today, automatically show tomorrow
            if (isToday && availableSlots.isEmpty()) {
                log.info("No slots available today, showing tomorrow's slots");
                return HandlerResponse.builder()
                    .message("")
                    .nextStep(ConversationStep.VIEW_TOMORROW_SLOTS)
                    .contextData(request.getContextData())
                    .build();
            }

            // If no slots for tomorrow either
            if (!isToday && availableSlots.isEmpty()) {
                return HandlerResponse.builder()
                    .message("❌ Sorry, we're fully booked for today and tomorrow.\n\n" +
                            "Please call us or try again tomorrow for next-day bookings.\n\n" +
                            "0️⃣ Main Menu")
                    .nextStep(ConversationStep.MAIN_MENU)
                    .clearContext(true)
                    .build();
            }

            // Handle user input (slot selection or MORE)
            String userInput = request.getUserInput().toUpperCase().trim();

            // Check for menu command
            if ("MENU".equals(userInput) || "0".equals(userInput)) {
                return HandlerResponse.builder()
                    .message("")
                    .nextStep(ConversationStep.MAIN_MENU)
                    .clearContext(true)
                    .build();
            }

            if ("MORE".equals(userInput) && isToday) {
                return HandlerResponse.builder()
                    .message("")
                    .nextStep(ConversationStep.VIEW_TOMORROW_SLOTS)
                    .contextData(request.getContextData())
                    .build();
            }

            Integer choice = request.getParsedChoice();
            if (choice != null && choice >= 1 && choice <= availableSlots.size()) {
                LocalTime selectedTime = availableSlots.get(choice - 1);

                // Update context with selected date and time
                context.put("booking_date", targetDate.toString());
                context.put("booking_time", selectedTime.toString());
                String contextJson = objectMapper.writeValueAsString(context);

                return HandlerResponse.builder()
                    .message("")
                    .nextStep(ConversationStep.CONFIRM_BOOKING)
                    .contextData(contextJson)
                    .build();
            }

            // Show slots menu
            String message = buildSlotsMessage(service, barber, availableSlots, targetDate, isToday);
            return HandlerResponse.builder()
                .message(message)
                .nextStep(request.getCurrentStep())
                .contextData(request.getContextData())
                .build();

        } catch (Exception e) {
            log.error("Error in ViewSlotsHandler", e);
            return HandlerResponse.builder()
                .message("⚠️ Something went wrong. Let's start over.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.VIEW_TODAY_SLOTS;
    }

    private Map<String, Object> parseContext(String contextData) throws Exception {
        if (contextData == null || contextData.isEmpty()) {
            return new HashMap<>();
        }
        return objectMapper.readValue(contextData, new TypeReference<>() {});
    }

    private String buildSlotsMessage(Service service, Barber barber, List<LocalTime> slots,
                                     LocalDate date, boolean isToday) {
        StringBuilder message = new StringBuilder();

        message.append(String.format("🪒 *%s* with *%s*\n\n", service.getName(), barber.getName()));

        String dayLabel = isToday ? "TODAY" : "TOMORROW";
        String formattedDate = date.format(DateTimeFormatter.ofPattern("EEE dd MMM"));

        if (isToday && slots.isEmpty()) {
            message.append(String.format("❌ Sorry, we're fully booked %s\n\n", dayLabel));
            message.append("Type MORE to see tomorrow's availability");
            return message.toString();
        }

        message.append(String.format("📅 *%s (%s)*:\n", dayLabel, formattedDate));

        for (int i = 0; i < slots.size(); i++) {
            LocalTime slot = slots.get(i);
            String formattedTime = slot.format(DateTimeFormatter.ofPattern("h:mm a"));
            message.append(String.format("%d️⃣ %s\n", i + 1, formattedTime));
        }

        message.append("\nType number to book");
        if (isToday) {
            message.append(" or MORE for tomorrow");
        }
        message.append("\n0️⃣ Main Menu");

        return message.toString();
    }
}
