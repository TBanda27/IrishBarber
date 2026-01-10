package com.banda.barbershop.scheduler;

import com.banda.barbershop.config.LoyaltyConfig;
import com.banda.barbershop.entity.Customer;
import com.banda.barbershop.service.CustomerService;
import com.banda.barbershop.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job for sending birthday messages to customers
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayScheduler {

    private final CustomerService customerService;
    private final WhatsAppService whatsAppService;
    private final LoyaltyConfig loyaltyConfig;

    /**
     * Send birthday messages to customers
     * Runs daily at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM
    public void sendBirthdayMessages() {
        if (!loyaltyConfig.isEnabled() || !loyaltyConfig.getBirthday().isEnabled()) {
            log.debug("Birthday messages are disabled");
            return;
        }

        log.info("Running birthday message job");

        try {
            List<Customer> birthdayCustomers = customerService.getTodaysBirthdays();

            if (birthdayCustomers.isEmpty()) {
                log.info("No birthdays today");
                return;
            }

            log.info("Found {} customers with birthdays today", birthdayCustomers.size());

            int sentCount = 0;
            for (Customer customer : birthdayCustomers) {
                try {
                    sendBirthdayMessage(customer);
                    customerService.markBirthdayMessageSent(customer.getPhoneNumber());

                    // Award birthday bonus points
                    awardBirthdayBonus(customer);

                    sentCount++;
                    log.info("Sent birthday message to customer {}", customer.getPhoneNumber());
                } catch (Exception e) {
                    log.error("Failed to send birthday message to {}: {}",
                             customer.getPhoneNumber(), e.getMessage(), e);
                }
            }

            log.info("Birthday message job completed: {} messages sent", sentCount);
        } catch (Exception e) {
            log.error("Error in birthday message job: {}", e.getMessage(), e);
        }
    }

    private void sendBirthdayMessage(Customer customer) {
        int bonusPoints = loyaltyConfig.getBirthday().getBonusPoints();
        String discountCode = loyaltyConfig.getBirthday().getDiscountCode();
        int discount = loyaltyConfig.getBirthday().getDiscountPercent();

        String name = customer.getName() != null ? customer.getName() : "there";

        String message = String.format("""
            🎉 *HAPPY BIRTHDAY %s!* 🎂

            The whole team at Fade Factory wishes you an amazing day!

            🎁 *Your Birthday Gift:*
            • %d Loyalty Points added
            • %d%% OFF your next booking
            • Use code: *%s*

            Book today to redeem your special birthday reward! 🎈

            Thank you for being part of our family! ❤️

            Reply 1 to book now!
            """,
            name.toUpperCase(),
            bonusPoints,
            discount,
            discountCode
        );

        whatsAppService.sendMessage(customer.getPhoneNumber(), message);
    }

    private void awardBirthdayBonus(Customer customer) {
        int bonusPoints = loyaltyConfig.getBirthday().getBonusPoints();
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + bonusPoints);
        customer.setLifetimeLoyaltyPoints(customer.getLifetimeLoyaltyPoints() + bonusPoints);
        log.info("Awarded {} birthday bonus points to customer {}",
                 bonusPoints, customer.getPhoneNumber());
    }
}
