package com.banda.barbershop.service;

import com.banda.barbershop.config.TwilioConfig;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final TwilioConfig twilioConfig;

    @PostConstruct
    public void init() {
        Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
        log.info("Twilio initialized successfully");
    }

    public void sendMessage(String toPhoneNumber, String messageBody) {
        try {
            Message message = Message.creator(
                new PhoneNumber("whatsapp:" + toPhoneNumber),
                new PhoneNumber("whatsapp:" + twilioConfig.getPhoneNumber()),
                messageBody
            ).create();

            log.info("Message sent successfully. SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", toPhoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send WhatsApp message", e);
        }
    }
}
