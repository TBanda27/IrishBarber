package com.banda.barbershop.service;

import com.banda.barbershop.config.BarberShopConfig;
import com.banda.barbershop.config.ReminderConfig;
import com.banda.barbershop.entity.Booking;
import com.banda.barbershop.entity.Booking.BookingStatus;
import com.banda.barbershop.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    private final BookingRepository bookingRepository;
    private final WhatsAppService whatsAppService;
    private final ReminderConfig reminderConfig;
    private final BarberShopConfig shopConfig;

    /**
     * Send day-before reminders for tomorrow's bookings
     * Should be called around 6 PM daily
     */
    @Transactional
    public int sendDayBeforeReminders() {
        if (!reminderConfig.isEnabled() || !reminderConfig.getDayBefore().isEnabled()) {
            log.debug("Day-before reminders are disabled");
            return 0;
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Find all confirmed bookings for tomorrow that haven't received day-before reminder
        List<Booking> bookings = bookingRepository.findByBookingDateAndStatus(tomorrow, BookingStatus.CONFIRMED)
            .stream()
            .filter(b -> !b.isDayBeforeReminderSent())
            .toList();

        log.info("Found {} bookings for tomorrow requiring day-before reminders", bookings.size());

        int sentCount = 0;
        for (Booking booking : bookings) {
            try {
                sendDayBeforeReminder(booking);
                booking.setDayBeforeReminderSent(true);
                booking.setDayBeforeReminderSentAt(LocalDateTime.now());
                bookingRepository.save(booking);
                sentCount++;
                log.info("Sent day-before reminder for booking {}", booking.getBookingCode());
            } catch (Exception e) {
                log.error("Failed to send day-before reminder for booking {}: {}",
                         booking.getBookingCode(), e.getMessage(), e);
            }
        }

        log.info("Sent {} day-before reminders", sentCount);
        return sentCount;
    }

    /**
     * Send one-hour-before reminders for upcoming bookings
     * Should be called every 5-10 minutes
     */
    @Transactional
    public int sendOneHourReminders() {
        if (!reminderConfig.isEnabled() || !reminderConfig.getOneHour().isEnabled()) {
            log.debug("One-hour reminders are disabled");
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        int minutesBefore = reminderConfig.getOneHour().getMinutesBefore();
        LocalDateTime targetTime = now.plusMinutes(minutesBefore);

        // Find bookings happening around target time (within a window)
        LocalTime startWindow = targetTime.toLocalTime().minusMinutes(10);
        LocalTime endWindow = targetTime.toLocalTime().plusMinutes(10);

        List<Booking> bookings = bookingRepository.findByBookingDateAndStatus(today, BookingStatus.CONFIRMED)
            .stream()
            .filter(b -> !b.isOneHourReminderSent())
            .filter(b -> isWithinTimeWindow(b.getStartTime(), startWindow, endWindow))
            .toList();

        log.info("Found {} bookings requiring one-hour reminders", bookings.size());

        int sentCount = 0;
        for (Booking booking : bookings) {
            try {
                sendOneHourReminder(booking);
                booking.setOneHourReminderSent(true);
                booking.setOneHourReminderSentAt(LocalDateTime.now());
                bookingRepository.save(booking);
                sentCount++;
                log.info("Sent one-hour reminder for booking {}", booking.getBookingCode());
            } catch (Exception e) {
                log.error("Failed to send one-hour reminder for booking {}: {}",
                         booking.getBookingCode(), e.getMessage(), e);
            }
        }

        log.info("Sent {} one-hour reminders", sentCount);
        return sentCount;
    }

    private void sendDayBeforeReminder(Booking booking) {
        String message = buildDayBeforeReminderMessage(booking);
        whatsAppService.sendMessage(booking.getCustomerPhone(), message);
    }

    private void sendOneHourReminder(Booking booking) {
        String message = buildOneHourReminderMessage(booking);
        whatsAppService.sendMessage(booking.getCustomerPhone(), message);
    }

    private String buildDayBeforeReminderMessage(Booking booking) {
        String formattedDate = booking.getBookingDate()
            .format(DateTimeFormatter.ofPattern("EEE dd MMM"));
        String formattedTime = booking.getStartTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"));

        return String.format("""
            📅 *Reminder: Appointment Tomorrow*

            You have a booking tomorrow!

            🪒 %s
            📅 Tomorrow (%s) at %s
            ⏱️ %d minutes
            📍 %s

            Booking Code: *#%s*

            To cancel, reply MENU and select option 3

            See you tomorrow! 👍
            """,
            booking.getService().getName(),
            formattedDate,
            formattedTime,
            booking.getService().getDurationMinutes(),
            shopConfig.getAddress(),
            booking.getBookingCode()
        );
    }

    private String buildOneHourReminderMessage(Booking booking) {
        String formattedTime = booking.getStartTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"));

        LocalDateTime appointmentTime = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        long minutesUntil = java.time.Duration.between(LocalDateTime.now(), appointmentTime).toMinutes();

        return String.format("""
            ⏰ *Reminder: Appointment in %d minutes*

            🪒 %s
            📅 TODAY at %s
            📍 %s

            Booking Code: *#%s*

            To cancel, reply MENU and select option 3

            See you soon! 👍
            """,
            minutesUntil,
            booking.getService().getName(),
            formattedTime,
            shopConfig.getAddress(),
            booking.getBookingCode()
        );
    }

    private boolean isWithinTimeWindow(LocalTime bookingTime, LocalTime startWindow, LocalTime endWindow) {
        return !bookingTime.isBefore(startWindow) && !bookingTime.isAfter(endWindow);
    }

    /**
     * Manual trigger for testing reminders
     */
    public void sendTestReminder(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingCode));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Can only send reminders for confirmed bookings");
        }

        log.info("Sending test reminder for booking {}", bookingCode);
        sendOneHourReminder(booking);
    }
}
