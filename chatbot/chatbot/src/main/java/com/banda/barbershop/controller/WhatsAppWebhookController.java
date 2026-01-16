package com.banda.barbershop.controller;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.entity.ConversationState;
import com.banda.barbershop.handler.MessageHandlerDispatcher;
import com.banda.barbershop.service.ConversationStateService;
import com.banda.barbershop.service.TwilioValidationService;
import com.banda.barbershop.service.WhatsAppService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private static final String EMPTY_TWIML_RESPONSE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>";

    private final MessageHandlerDispatcher dispatcher;
    private final ConversationStateService stateService;
    private final WhatsAppService whatsAppService;
    private final TwilioValidationService twilioValidationService;

    @PostMapping(value = "/whatsapp", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receiveMessage(
            HttpServletRequest httpRequest,
            @RequestParam("From") String from,
            @RequestParam("Body") String body) {

        // Validate Twilio signature
        if (!twilioValidationService.validateRequest(httpRequest)) {
            log.warn("Rejected request with invalid Twilio signature from IP: {}",
                    getClientIp(httpRequest));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(EMPTY_TWIML_RESPONSE);
        }

        log.info("Received message from {}: {}", from, body);

        if (from == null || from.isEmpty()) {
            log.error("Invalid webhook: 'From' parameter is missing");
            return ResponseEntity.ok(EMPTY_TWIML_RESPONSE);
        }

        if (body == null) {
            log.warn("Received empty message body from {}", from);
            body = "";
        }

        String phoneNumber = extractPhoneNumber(from);

        ConversationState state;
        try {
            state = stateService.getOrCreate(phoneNumber);
            log.debug("Current conversation state for {}: Step={}, Context={}",
                     phoneNumber, state.getCurrentStep(), state.getContextData());
        } catch (Exception e) {
            log.error("Failed to retrieve conversation state for {}: {}", phoneNumber, e.getMessage(), e);
            whatsAppService.sendMessage(phoneNumber,
                "We're experiencing technical difficulties. Please try again in a moment.");
            return ResponseEntity.ok(EMPTY_TWIML_RESPONSE);
        }

        HandlerRequest request = HandlerRequest.builder()
            .phoneNumber(phoneNumber)
            .userInput(body.trim())
            .parsedChoice(parseChoice(body.trim()))
            .currentStep(state.getCurrentStep())
            .contextData(state.getContextData())
            .build();

        HandlerResponse response;
        try {
            response = dispatcher.dispatch(request);
            log.debug("Handler response: NextStep={}, ClearContext={}",
                     response.getNextStep(), response.isClearContext());
        } catch (Exception e) {
            log.error("Error dispatching to handler for {}: {}", phoneNumber, e.getMessage(), e);
            whatsAppService.sendMessage(phoneNumber,
                "Something went wrong. Let's start over!\n\n0️⃣ - Main Menu");
            stateService.resetToMainMenu(phoneNumber);
            return ResponseEntity.ok(EMPTY_TWIML_RESPONSE);
        }

        try {
            if (response.isClearContext()) {
                stateService.updateStepAndContext(phoneNumber, response.getNextStep(), null);
            } else if (response.getContextData() != null) {
                stateService.updateStepAndContext(phoneNumber, response.getNextStep(), response.getContextData());
            } else {
                stateService.updateStep(phoneNumber, response.getNextStep());
            }
        } catch (Exception e) {
            log.error("Failed to update conversation state for {}: {}", phoneNumber, e.getMessage(), e);
        }

        // Auto-dispatch: if handler returned empty message, call next handler to show content
        if (response.getMessage().isEmpty() && response.getNextStep() != null) {
            try {
                ConversationState updatedState = stateService.getOrCreate(phoneNumber);
                HandlerRequest followUpRequest = HandlerRequest.builder()
                    .phoneNumber(phoneNumber)
                    .userInput("")
                    .parsedChoice(null)
                    .currentStep(updatedState.getCurrentStep())
                    .contextData(updatedState.getContextData())
                    .build();

                response = dispatcher.dispatch(followUpRequest);
                log.debug("Auto-dispatch triggered - empty message from previous handler. NextStep={}", response.getNextStep());

                if (response.isClearContext()) {
                    stateService.updateStepAndContext(phoneNumber, response.getNextStep(), null);
                } else if (response.getContextData() != null) {
                    stateService.updateStepAndContext(phoneNumber, response.getNextStep(), response.getContextData());
                } else {
                    stateService.updateStep(phoneNumber, response.getNextStep());
                }
            } catch (Exception e) {
                log.error("Error in auto-dispatch for {}: {}", phoneNumber, e.getMessage(), e);
            }
        }

        if (!response.getMessage().isEmpty()) {
            try {
                whatsAppService.sendMessage(phoneNumber, response.getMessage());
                log.info("Message sent successfully to {}", phoneNumber);
            } catch (Exception e) {
                log.error("Failed to send message to {}: {}", phoneNumber, e.getMessage(), e);
            }
        }

        return ResponseEntity.ok(EMPTY_TWIML_RESPONSE);
    }

    private String extractPhoneNumber(String from) {
        return from.replace("whatsapp:", "");
    }

    private Integer parseChoice(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
