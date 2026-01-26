package com.banda.barbershop.handler;

import com.banda.barbershop.config.BarberShopConfig;
import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.entity.Barber;
import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.entity.Service;
import com.banda.barbershop.enums.ConversationStep;
import com.banda.barbershop.repository.BarberRepository;
import com.banda.barbershop.repository.ServiceRepository;
import com.banda.barbershop.service.BookingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmBookingHandler implements MessageHandler {

    private final BookingService bookingService;
    private final ServiceRepository serviceRepository;
    private final BarberRepository barberRepository;
    private final BarberShopConfig shopConfig;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.CONFIRM_BOOKING ||
               step == ConversationStep.BOOKING_CONFIRMED;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        try {
            // Handle BOOKING_CONFIRMED state (just show confirmation)
            if (request.getCurrentStep() == ConversationStep.BOOKING_CONFIRMED) {
                Map<String, Object> context = parseContext(request.getContextData());
                String bookingCode = (String) context.get("booking_code");
                LocalDate bookingDate = LocalDate.parse((String) context.get("booking_date"));
                LocalTime bookingTime = LocalTime.parse((String) context.get("booking_time"));
                Long serviceId = ((Number) context.get("service_id")).longValue();
                Long barberId = ((Number) context.get("barber_id")).longValue();

                Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new IllegalStateException("Service not found"));

                Barber barber = barberRepository.findById(barberId)
                    .orElseThrow(() -> new IllegalStateException("Barber not found"));

                String confirmationMessage = buildConfirmationMessage(
                    bookingCode, service, barber, bookingDate, bookingTime);

                return HandlerResponse.builder()
                    .message(confirmationMessage)
                    .nextStep(ConversationStep.MAIN_MENU)
                    .clearContext(true)
                    .build();
            }

            // Handle CONFIRM_BOOKING state
            Map<String, Object> context = parseContext(request.getContextData());
            Long serviceId = ((Number) context.get("service_id")).longValue();
            Long barberId = ((Number) context.get("barber_id")).longValue();
            LocalDate bookingDate = LocalDate.parse((String) context.get("booking_date"));
            LocalTime bookingTime = LocalTime.parse((String) context.get("booking_time"));

            Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalStateException("Service not found"));

            Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new IllegalStateException("Barber not found"));

            // Show confirmation prompt on first entry
            String userInput = request.getUserInput().toUpperCase().trim();

            if (userInput.equals("YES")) {
                // Create the booking
                try {
                    Booking booking = bookingService.createBooking(
                        request.getPhoneNumber(),
                        service,
                        barberId,
                        bookingDate,
                        bookingTime
                    );

                    // Store booking code in context
                    context.put("booking_code", booking.getBookingCode());
                    String contextJson = objectMapper.writeValueAsString(context);

                    return HandlerResponse.builder()
                        .message("")
                        .nextStep(ConversationStep.BOOKING_CONFIRMED)
                        .contextData(contextJson)
                        .build();

                } catch (IllegalStateException e) {
                    log.error("Booking failed: {}", e.getMessage());
                    return HandlerResponse.builder()
                        .message("❌ Sorry, that time slot was just taken!\n\n" +
                                "Let's try another time.\n\n0️⃣ Main Menu")
                        .nextStep(ConversationStep.MAIN_MENU)
                        .clearContext(true)
                        .build();
                }

            } else if (userInput.equals("CANCEL") || userInput.equals("0")) {
                return HandlerResponse.builder()
                    .message("Booking cancelled.\n\n0️⃣ Main Menu")
                    .nextStep(ConversationStep.MAIN_MENU)
                    .clearContext(true)
                    .build();
            }

            // Show confirmation message
            String confirmMessage = buildConfirmationPrompt(service, barber, bookingDate, bookingTime);
            return HandlerResponse.builder()
                .message(confirmMessage)
                .nextStep(ConversationStep.CONFIRM_BOOKING)
                .contextData(request.getContextData())
                .build();

        } catch (Exception e) {
            log.error("Error in ConfirmBookingHandler", e);
            return HandlerResponse.builder()
                .message("⚠️ Something went wrong. Let's start over.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.CONFIRM_BOOKING;
    }

    private Map<String, Object> parseContext(String contextData) throws Exception {
        if (contextData == null || contextData.isEmpty()) {
            return new HashMap<>();
        }
        return objectMapper.readValue(contextData, new TypeReference<>() {});
    }

    private String buildConfirmationPrompt(Service service, Barber barber, LocalDate date, LocalTime time) {
        String dayLabel = getDayLabel(date);
        String formattedDate = date.format(DateTimeFormatter.ofPattern("EEE dd MMM"));
        String formattedTime = time.format(DateTimeFormatter.ofPattern("h:mm a"));

        return String.format("""
            ✅ *Confirm Your Booking*

            🪒 %s
            👨‍🦲 With %s
            📅 %s (%s) at %s
            ⏱️ %d minutes
            💰 €%.0f
            📍 %s

            Reply *YES* to confirm or *CANCEL* to restart
            """,
            service.getName(),
            barber.getName(),
            dayLabel,
            formattedDate,
            formattedTime,
            service.getDurationMinutes(),
            service.getPrice(),
            shopConfig.getAddress()
        );
    }

    private String buildConfirmationMessage(String bookingCode, Service service, Barber barber,
                                           LocalDate date, LocalTime time) {
        String dayLabel = getDayLabel(date);
        String formattedDate = date.format(DateTimeFormatter.ofPattern("EEE dd MMM"));
        String formattedTime = time.format(DateTimeFormatter.ofPattern("h:mm a"));

        return String.format("""
            ✅ *BOOKING CONFIRMED!*

            Booking Code: *#%s*

            🪒 %s
            👨‍🦲 With %s
            📅 %s (%s) at %s
            📍 %s

            See you soon! 👍

            To cancel: Reply *4* from main menu

            0️⃣ Main Menu
            """,
            bookingCode,
            service.getName(),
            barber.getName(),
            dayLabel,
            formattedDate,
            formattedTime,
            shopConfig.getAddress()
        );
    }

    private String getDayLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "TODAY";
        } else if (date.equals(today.plusDays(1))) {
            return "TOMORROW";
        } else {
            return date.format(DateTimeFormatter.ofPattern("EEEE")).toUpperCase();
        }
    }
}
