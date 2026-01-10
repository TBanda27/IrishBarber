package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.enums.ConversationStep;
import com.banda.barbershop.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CancelBookingHandler implements MessageHandler {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.CANCEL_BOOKING_INPUT ||
               step == ConversationStep.CANCEL_BOOKING_CONFIRM;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        try {
            if (request.getCurrentStep() == ConversationStep.CANCEL_BOOKING_INPUT) {
                // Show initial prompt if just entering this state
                if ("show_initial".equals(request.getContextData())) {
                    return HandlerResponse.builder()
                        .message("""
                            ❌ *Cancel a Booking*

                            Please enter your booking code (e.g., BK1234 or #BK1234)

                            0️⃣ Main Menu
                            """)
                        .nextStep(ConversationStep.CANCEL_BOOKING_INPUT)
                        .clearContext(true)
                        .build();
                }
                return handleBookingCodeInput(request);
            } else {
                return handleCancellationConfirmation(request);
            }
        } catch (Exception e) {
            log.error("Error in CancelBookingHandler", e);
            return HandlerResponse.builder()
                .message("⚠️ Something went wrong. Let's start over.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.CANCEL_BOOKING_INPUT;
    }

    private HandlerResponse handleBookingCodeInput(HandlerRequest request) {
        String userInput = request.getUserInput().trim().toUpperCase();

        // Check for menu command
        if ("MENU".equals(userInput) || "0".equals(userInput)) {
            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.MAIN_MENU)
                .contextData("show_initial")
                .build();
        }

        // Extract booking code (remove # if present)
        String bookingCode = userInput.replace("#", "").replace("BK", "");
        bookingCode = "BK" + bookingCode;

        // Validate format (BK followed by 4 digits)
        if (!bookingCode.matches("BK\\d{4}")) {
            return HandlerResponse.builder()
                .message("""
                    ❌ Invalid booking code format.

                    Please enter your booking code (e.g., BK1234 or #BK1234)

                    0️⃣ Main Menu
                    """)
                .nextStep(ConversationStep.CANCEL_BOOKING_INPUT)
                .clearContext(true)
                .build();
        }

        // Check if booking exists
        Optional<Booking> bookingOpt = bookingService.getBookingByCode(bookingCode);

        if (bookingOpt.isEmpty()) {
            return HandlerResponse.builder()
                .message(String.format("""
                    ❌ Booking *#%s* not found.

                    Please check your booking code and try again.

                    0️⃣ Main Menu
                    """, bookingCode))
                .nextStep(ConversationStep.CANCEL_BOOKING_INPUT)
                .clearContext(true)
                .build();
        }

        Booking booking = bookingOpt.get();

        // Verify ownership
        if (!booking.getCustomerPhone().equals(request.getPhoneNumber())) {
            return HandlerResponse.builder()
                .message("""
                    ❌ This booking doesn't belong to your number.

                    0️⃣ Main Menu
                    """)
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        // Check if already cancelled
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            return HandlerResponse.builder()
                .message(String.format("""
                    ℹ️ Booking *#%s* is already cancelled.

                    0️⃣ Main Menu
                    """, bookingCode))
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        // Show confirmation prompt
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("booking_code", bookingCode);
            String contextJson = objectMapper.writeValueAsString(context);

            String confirmMessage = buildCancellationPrompt(booking);

            return HandlerResponse.builder()
                .message(confirmMessage)
                .nextStep(ConversationStep.CANCEL_BOOKING_CONFIRM)
                .contextData(contextJson)
                .build();

        } catch (Exception e) {
            log.error("Failed to serialize context", e);
            throw new RuntimeException(e);
        }
    }

    private HandlerResponse handleCancellationConfirmation(HandlerRequest request) throws Exception {
        String userInput = request.getUserInput().toUpperCase().trim();

        Map<String, Object> context = objectMapper.readValue(
            request.getContextData(),
            Map.class
        );
        String bookingCode = (String) context.get("booking_code");

        if ("YES".equals(userInput)) {
            boolean cancelled = bookingService.cancelBooking(bookingCode, request.getPhoneNumber());

            if (cancelled) {
                return HandlerResponse.builder()
                    .message(String.format("""
                        ✅ *Booking Cancelled*

                        Booking *#%s* has been cancelled successfully.

                        Hope to see you again soon! 👍

                        0️⃣ Main Menu
                        """, bookingCode))
                    .nextStep(ConversationStep.MAIN_MENU)
                    .clearContext(true)
                    .build();
            } else {
                return HandlerResponse.builder()
                    .message("⚠️ Failed to cancel booking. Please try again.\n\n0️⃣ Main Menu")
                    .nextStep(ConversationStep.MAIN_MENU)
                    .clearContext(true)
                    .build();
            }

        } else if ("NO".equals(userInput) || "CANCEL".equals(userInput) || "0".equals(userInput)) {
            return HandlerResponse.builder()
                .message("Cancellation aborted. Your booking is still active.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        // Invalid input - show prompt again
        Optional<Booking> booking = bookingService.getBookingByCode(bookingCode);
        if (booking.isPresent()) {
            String confirmMessage = buildCancellationPrompt(booking.get());
            return HandlerResponse.builder()
                .message(confirmMessage + "\n\n⚠️ Please reply YES or NO")
                .nextStep(ConversationStep.CANCEL_BOOKING_CONFIRM)
                .contextData(request.getContextData())
                .build();
        } else {
            return HandlerResponse.builder()
                .message("⚠️ Booking not found.\n\n0️⃣ Main Menu")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }
    }

    private String buildCancellationPrompt(Booking booking) {
        String dayLabel = booking.getBookingDate().equals(java.time.LocalDate.now()) ? "TODAY" : "TOMORROW";
        String formattedDate = booking.getBookingDate()
            .format(DateTimeFormatter.ofPattern("EEE dd MMM"));
        String formattedTime = booking.getStartTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"));

        return String.format("""
            ⚠️ *Confirm Cancellation*

            Booking Code: *#%s*
            🪒 %s
            📅 %s (%s) at %s

            Are you sure you want to cancel this booking?

            Reply *YES* to cancel or *NO* to keep it
            """,
            booking.getBookingCode(),
            booking.getService().getName(),
            dayLabel,
            formattedDate,
            formattedTime
        );
    }
}
