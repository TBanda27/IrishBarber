package com.banda.barbershop.service;

import com.banda.barbershop.config.TwilioConfig;
import com.twilio.security.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TwilioValidationService {

    private final RequestValidator requestValidator;
    private final boolean validationEnabled;

    public TwilioValidationService(
            TwilioConfig twilioConfig,
            @Value("${twilio.webhook.validation-enabled:true}") boolean validationEnabled) {
        this.requestValidator = new RequestValidator(twilioConfig.getAuthToken());
        this.validationEnabled = validationEnabled;
        log.info("Twilio webhook validation is {}", validationEnabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Validate that the request came from Twilio
     *
     * @param request The HTTP request
     * @return true if valid or validation is disabled, false otherwise
     */
    public boolean validateRequest(HttpServletRequest request) {
        if (!validationEnabled) {
            log.debug("Twilio validation disabled, skipping check");
            return true;
        }

        String signature = request.getHeader("X-Twilio-Signature");
        if (signature == null || signature.isEmpty()) {
            log.warn("Missing X-Twilio-Signature header");
            return false;
        }

        String url = buildRequestUrl(request);
        Map<String, String> params = extractParams(request);

        boolean isValid = requestValidator.validate(url, params, signature);

        if (!isValid) {
            log.warn("Invalid Twilio signature for request to {}", url);
        } else {
            log.debug("Twilio signature validated successfully");
        }

        return isValid;
    }

    /**
     * Build the full URL as Twilio sees it
     */
    private String buildRequestUrl(HttpServletRequest request) {
        // Check for forwarded headers (when behind proxy/ngrok)
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");

        String scheme = forwardedProto != null ? forwardedProto : request.getScheme();
        String host = forwardedHost != null ? forwardedHost : request.getServerName();
        int port = request.getServerPort();
        String path = request.getRequestURI();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(host);

        // Only add port if it's non-standard
        if ((scheme.equals("http") && port != 80) ||
            (scheme.equals("https") && port != 443)) {
            // Don't add port if using forwarded headers (proxy handles it)
            if (forwardedHost == null) {
                url.append(":").append(port);
            }
        }

        url.append(path);

        return url.toString();
    }

    /**
     * Extract POST parameters as a map
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return Collections.emptyMap();
        }

        return parameterMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() != null && e.getValue().length > 0 ? e.getValue()[0] : ""
            ));
    }
}
