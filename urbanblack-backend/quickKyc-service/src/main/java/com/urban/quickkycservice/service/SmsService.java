package com.urban.quickkycservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    @Value("${sms.authkey}")
    private String authKey;

    @Value("${sms.sender}")
    private String sender;

    @Value("${sms.route}")
    private String route;

    @Value("${sms.country}")
    private String country;

    @Value("${sms.dlt.template-id}")
    private String templateId;

    @Value("${sms.base-url}")
    private String baseUrl;

    @Value("${sms.dlt.entity-id:}")
    private String entityId;

    @Value("${sms.dlt-template}")
    private String dltTemplate;

    private final RestTemplate restTemplate;

    public boolean sendOtpSms(String mobile, String otp) {
        if (mobile == null || mobile.isBlank() || (mobile.length() != 10 && mobile.length() != 12)) {
            log.error("Invalid mobile number for SMS delivery: {}", mobile);
            return false;
        }

        // Clean mobile number (strip 91 if 12 digits)
        if (mobile.length() == 12 && mobile.startsWith("91")) {
            mobile = mobile.substring(2);
        }

        String message = dltTemplate.replace("{otp}", otp);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("authkey", authKey)
                .queryParam("mobiles", mobile)
                .queryParam("message", message)
                .queryParam("sender", sender)
                .queryParam("route", route)
                .queryParam("country", country)
                .queryParam("DLT_TE_ID", templateId);

        // Add entityid only if it's set and not a placeholder
        if (entityId != null && !entityId.isEmpty() && !entityId.contains("YOUR_19_DIGIT")) {
            builder.queryParam("entityid", entityId);
        }

        String url = builder.build().toUriString();
        String maskedUrl = url.replace(authKey, "REDACTED");
        
        log.info(">>> Sending SMS to {} via BestSMS...", mobile);
        log.info(">>> SMS URL: {}", maskedUrl);
        log.info(">>> SMS Message Content: [{}]", message);

        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info(">>> BestSMS Response: {}", response);

            if (response == null || response.isBlank()) {
                log.error("SMS provider returned empty response");
                return false;
            }

            String normalizedResponse = response.toLowerCase();
            if (normalizedResponse.contains("error")
                    || normalizedResponse.contains("invalid")
                    || normalizedResponse.contains("failed")
                    || normalizedResponse.contains("not submitted")) {
                log.error("SMS provider rejected OTP request: {}", response);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error(">>> Failed to send SMS via BestSMS: {}", e.getMessage());
            return false;
        }
    }
}
