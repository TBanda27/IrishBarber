package com.banda.barbershop.controller;

import com.banda.barbershop.config.BarberShopConfig;
import com.banda.barbershop.config.TwilioConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final BarberShopConfig shopConfig;
    private final TwilioConfig twilioConfig;

    @GetMapping("/shop")
    public BarberShopConfig getShopInfo() {
        return shopConfig;
    }

    @GetMapping("/shop/basic")
    public Map<String, String> getBasicInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("name", shopConfig.getName());
        info.put("address", shopConfig.getAddress());
        info.put("phone", shopConfig.getPhone());
        info.put("whatsappNumber", shopConfig.getWhatsappNumber());
        return info;
    }

    @GetMapping("/shop/hours")
    public Map<String, String> getHoursInfo() {
        Map<String, String> hours = new HashMap<>();
        hours.put("openingTime", shopConfig.getOperatingHours().getOpeningTime().toString());
        hours.put("closingTime", shopConfig.getOperatingHours().getClosingTime().toString());
        hours.put("closedDays", shopConfig.getOperatingHours().getClosedDays().toString());
        return hours;
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("message", "Barbershop Config loaded successfully!");
        health.put("shopName", shopConfig.getName());
        return health;
    }

    @GetMapping("/twilio")
    public Map<String, String> getTwilioInfo() {
        Map<String, String> twilio = new HashMap<>();
        // Mask sensitive data for security
        twilio.put("accountSid", maskSensitive(twilioConfig.getAccountSid()));
        twilio.put("authToken", maskSensitive(twilioConfig.getAuthToken()));
        twilio.put("phoneNumber", twilioConfig.getPhoneNumber());
        twilio.put("status", "Twilio config loaded ✅");
        return twilio;
    }

    @GetMapping("/twilio/test")
    public Map<String, String> testTwilioConnection() {
        Map<String, String> result = new HashMap<>();

        boolean configValid = twilioConfig.getAccountSid() != null
                && twilioConfig.getAuthToken() != null
                && twilioConfig.getPhoneNumber() != null;

        result.put("configLoaded", String.valueOf(configValid));
        result.put("phoneNumber", twilioConfig.getPhoneNumber());
        result.put("message", configValid ? "Twilio config is valid ✅" : "Twilio config is missing ❌");

        return result;
    }

    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
