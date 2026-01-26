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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SelectDateHandler implements MessageHandler {

    private static final int MAX_BOOKING_DAYS = 7;
    private static final String[] NUMBER_EMOJIS = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};

    private final AvailabilityService availabilityService;
    private final ServiceRepository serviceRepository;
    private final BarberRepository barberRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.SELECT_DATE;
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

            // Get available dates for the next 7 days
            Map<LocalDate, Integer> availableDates = availabilityService.getAvailableDatesForBarber(
                service, barberId, MAX_BOOKING_DAYS);

            // If no dates available in the next 7 days
            if (availableDates.isEmpty()) {
                return HandlerResponse.builder()
                    .message("❌ Sorry, " + barber.getName() + " is fully booked for the next 7 days.\n\n" +
                            "Please try another barber or check back later.\n\n" +
                            "0️⃣ Main Menu")
                    .nextStep(ConversationStep.MAIN_MENU)
                    .clearContext(true)
                    .build();
            }

            // Handle menu command
            String userInput = request.getUserInput().toUpperCase().trim();
            if ("MENU".equals(userInput) || "0".equals(userInput)) {
                return HandlerResponse.builder()
                    .message("")
                    .nextStep(ConversationStep.MAIN_MENU)
                    .clearContext(true)
                    .build();
            }

            // Convert map to list for indexed access
            List<LocalDate> dateList = new ArrayList<>(availableDates.keySet());

            // Handle date selection
            Integer choice = request.getParsedChoice();
            if (choice != null && choice >= 1 && choice <= dateList.size()) {
                LocalDate selectedDate = dateList.get(choice - 1);

                // Store selected date in context
                context.put("booking_date", selectedDate.toString());
                String contextJson = objectMapper.writeValueAsString(context);

                log.info("Customer {} selected date: {}", request.getPhoneNumber(), selectedDate);

                return HandlerResponse.builder()
                    .message("") // SelectTimeHandler will show time slots
                    .nextStep(ConversationStep.SELECT_TIME)
                    .contextData(contextJson)
                    .build();
            }

            // Show dates menu
            String message = buildDateMenu(service, barber, availableDates);
            return HandlerResponse.builder()
                .message(message)
                .nextStep(ConversationStep.SELECT_DATE)
                .contextData(request.getContextData())
                .build();

        } catch (Exception e) {
            log.error("Error in SelectDateHandler", e);
            return HandlerResponse.builder()
                .message("⚠️ Something went wrong. Let's start over.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.SELECT_DATE;
    }

    private Map<String, Object> parseContext(String contextData) throws Exception {
        if (contextData == null || contextData.isEmpty()) {
            return new HashMap<>();
        }
        return objectMapper.readValue(contextData, new TypeReference<>() {});
    }

    private String buildDateMenu(Service service, Barber barber, Map<LocalDate, Integer> availableDates) {
        StringBuilder message = new StringBuilder();

        message.append(String.format("🪒 *%s* with *%s*\n\n", service.getName(), barber.getName()));
        message.append("📅 *Available Dates:*\n\n");

        LocalDate today = LocalDate.now();
        int index = 0;

        for (Map.Entry<LocalDate, Integer> entry : availableDates.entrySet()) {
            LocalDate date = entry.getKey();
            int slotCount = entry.getValue();

            String dayLabel;
            if (date.equals(today)) {
                dayLabel = "Today";
            } else if (date.equals(today.plusDays(1))) {
                dayLabel = "Tomorrow";
            } else {
                dayLabel = date.format(DateTimeFormatter.ofPattern("EEEE"));
            }

            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM"));
            String slotText = slotCount == 1 ? "slot" : "slots";

            String emoji = index < NUMBER_EMOJIS.length ? NUMBER_EMOJIS[index] : (index + 1) + "️⃣";
            message.append(String.format("%s %s (%s) - %d %s\n",
                emoji, dayLabel, formattedDate, slotCount, slotText));

            index++;
        }

        message.append("\nReply with a number to select a date");
        message.append("\n0️⃣ Main Menu");

        return message.toString();
    }
}
