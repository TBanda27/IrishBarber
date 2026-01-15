package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.enums.ConversationStep;
import com.banda.barbershop.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewMyBookingsHandler implements MessageHandler {

    private final BookingService bookingService;

    @Override
    public boolean canHandle(ConversationStep step) {
        return step == ConversationStep.VIEW_MY_BOOKINGS;
    }

    @Override
    public HandlerResponse handle(HandlerRequest request) {
        // Check for menu command - only 0 should go back to main menu
        String userInput = request.getUserInput().toUpperCase().trim();
        if ("0".equals(userInput) || "MENU".equals(userInput)) {
            return HandlerResponse.builder()
                .message("")
                .nextStep(ConversationStep.MAIN_MENU)
                .clearContext(true)
                .build();
        }

        List<Booking> bookings = bookingService.getCustomerBookings(request.getPhoneNumber());

        if (bookings.isEmpty()) {
            return HandlerResponse.builder()
                .message("""
                    📋 *Your Bookings*

                    You have no active bookings.

                    0️⃣ Main Menu
                    """)
                .nextStep(ConversationStep.VIEW_MY_BOOKINGS)
                .clearContext(true)
                .build();
        }

        // Show bookings and stay in this step until user presses 0
        String message = buildBookingsMessage(bookings);
        return HandlerResponse.builder()
            .message(message)
            .nextStep(ConversationStep.VIEW_MY_BOOKINGS)
            .clearContext(true)
            .build();
    }

    @Override
    public ConversationStep getHandledStep() {
        return ConversationStep.VIEW_MY_BOOKINGS;
    }

    private String buildBookingsMessage(List<Booking> bookings) {
        StringBuilder message = new StringBuilder();
        message.append("📋 *Your Active Bookings*\n\n");

        for (Booking booking : bookings) {
            String dayLabel = getDayLabel(booking.getBookingDate());
            String formattedDate = booking.getBookingDate()
                .format(DateTimeFormatter.ofPattern("EEE dd MMM"));
            String formattedTime = booking.getStartTime()
                .format(DateTimeFormatter.ofPattern("h:mm a"));

            message.append(String.format("""
                *#%s*
                🪒 %s
                📅 %s (%s) at %s

                """,
                booking.getBookingCode(),
                booking.getService().getName(),
                dayLabel,
                formattedDate,
                formattedTime
            ));
        }

        message.append("To cancel a booking, reply *3* from main menu\n\n");
        message.append("0️⃣ Main Menu");

        return message.toString();
    }

    private String getDayLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "TODAY";
        } else if (date.equals(today.plusDays(1))) {
            return "TOMORROW";
        } else {
            return date.format(DateTimeFormatter.ofPattern("EEE"));
        }
    }
}
