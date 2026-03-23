package com.urbanblack.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class OtpService {

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

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOtpSms(String mobile, String otp) {
        if (mobile == null || mobile.isBlank() || mobile.length() != 10) {
            throw new RuntimeException("Invalid mobile number for SMS delivery");
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
        
        System.out.println(">>> Sending SMS to " + mobile + " via BestSMS...");
        System.out.println(">>> SMS URL: " + maskedUrl);
        System.out.println(">>> SMS Message Content: [" + message + "]");

        try {
            String response = restTemplate.getForObject(url, String.class);
            System.out.println(">>> BestSMS Response: " + response);

            if (response == null || response.isBlank()) {
                throw new RuntimeException("SMS provider returned empty response");
            }

            String normalizedResponse = response.toLowerCase();
            if (normalizedResponse.contains("error")
                    || normalizedResponse.contains("invalid")
                    || normalizedResponse.contains("failed")
                    || normalizedResponse.contains("not submitted")) {
                throw new RuntimeException("SMS provider rejected OTP request: " + response);
            }
        } catch (Exception e) {
            System.err.println(">>> Failed to send SMS via BestSMS: " + e.getMessage());
            throw new RuntimeException("Failed to deliver OTP SMS", e);
        }
    }
}