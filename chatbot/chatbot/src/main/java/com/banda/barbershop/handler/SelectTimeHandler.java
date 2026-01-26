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
public class SelectTimeHandler implements MessageHandler {

    private static final String[] NUMBER_EMOJIS = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};

    private final AvailabilityService availabilityService;
    private final ServiceRepository serviceRepository;
    private final BarberRepository barberRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.SELECT_TIME;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        try {
            Map<String, Object> context = parseContext(request.getContextData());
            Long serviceId = ((Number) context.get("service_id")).longValue();
            Long barberId = ((Number) context.get("barber_id")).longValue();
            LocalDate bookingDate = LocalDate.parse((String) context.get("booking_date"));

            Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalStateException("Service not found"));

            Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new IllegalStateException("Barber not found"));

            // Get available slots for the selected date
            List<LocalTime> availableSlots = availabilityService.getAvailableSlotsForBarber(
                service, barberId, bookingDate);

            // If no slots available (edge case - date became fully booked)
            if (availableSlots.isEmpty()) {
                return HandlerResponse.builder()
                    .message("❌ Sorry, this date just became fully booked.\n\n" +
                            "Let's pick another date.\n\n" +
                            "0️⃣ Main Menu")
                    .nextStep(ConversationStep.SELECT_DATE)
                    .contextData(request.getContextData())
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

            // Handle BACK command to go back to date selection
            if ("BACK".equals(userInput)) {
                context.remove("booking_date");
                String contextJson = objectMapper.writeValueAsString(context);
                return HandlerResponse.builder()
                    .message("")
                    .nextStep(ConversationStep.SELECT_DATE)
                    .contextData(contextJson)
                    .build();
            }

            // Handle time slot selection
            Integer choice = request.getParsedChoice();
            if (choice != null && choice >= 1 && choice <= availableSlots.size()) {
                LocalTime selectedTime = availableSlots.get(choice - 1);

                // Store selected time in context
                context.put("booking_time", selectedTime.toString());
                String contextJson = objectMapper.writeValueAsString(context);

                log.info("Customer {} selected time: {} on {}",
                    request.getPhoneNumber(), selectedTime, bookingDate);

                return HandlerResponse.builder()
                    .message("") // ConfirmBookingHandler will show confirmation
                    .nextStep(ConversationStep.CONFIRM_BOOKING)
                    .contextData(contextJson)
                    .build();
            }

            // Show time slots menu
            String message = buildTimeSlotsMenu(service, barber, bookingDate, availableSlots);
            return HandlerResponse.builder()
                .message(message)
                .nextStep(ConversationStep.SELECT_TIME)
                .contextData(request.getContextData())
                .build();

        } catch (Exception e) {
            log.error("Error in SelectTimeHandler", e);
            return HandlerResponse.builder()
                .message("⚠️ Something went wrong. Let's start over.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.SELECT_TIME;
    }

    private Map<String, Object> parseContext(String contextData) throws Exception {
        if (contextData == null || contextData.isEmpty()) {
            return new HashMap<>();
        }
        return objectMapper.readValue(contextData, new TypeReference<>() {});
    }

    private String buildTimeSlotsMenu(Service service, Barber barber,
                                       LocalDate date, List<LocalTime> slots) {
        StringBuilder message = new StringBuilder();

        message.append(String.format("🪒 *%s* with *%s*\n\n", service.getName(), barber.getName()));

        String dayLabel = getDayLabel(date);
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM"));
        message.append(String.format("🕐 *Available Times for %s (%s):*\n\n", dayLabel, formattedDate));

        for (int i = 0; i < slots.size(); i++) {
            LocalTime slot = slots.get(i);
            String formattedTime = slot.format(DateTimeFormatter.ofPattern("h:mm a"));
            String emoji = i < NUMBER_EMOJIS.length ? NUMBER_EMOJIS[i] : (i + 1) + "️⃣";
            message.append(String.format("%s %s\n", emoji, formattedTime));
        }

        message.append("\nReply with a number to book");
        message.append("\nType BACK to select a different date");
        message.append("\n0️⃣ Main Menu");

        return message.toString();
    }

    private String getDayLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "Today";
        } else if (date.equals(today.plusDays(1))) {
            return "Tomorrow";
        } else {
            return date.format(DateTimeFormatter.ofPattern("EEEE"));
        }
    }
}
