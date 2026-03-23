package com.urban.quickkycservice.service;

import com.urban.quickkycservice.dto.RcAdvanceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnGridService {

    private final RestTemplate restTemplate;

    @Value("${ongrid.api.url}")
    private String apiUrl;

    @Value("${ongrid.api.key}")
    private String apiKey;

    @Value("${quickkyc.api.mock-mode:false}")
    private boolean mockMode;

    public Map<String, Object> verifyRc(RcAdvanceRequest request) {
        if (mockMode) {
            log.info("[MOCK-ONGRID] RC verification for: {}", request.getId_number());
            return mockRcResponse(request.getId_number());
        }

        log.info("Calling OnGrid RC API: {}", apiUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);

            Map<String, String> body = new HashMap<>();
            body.put("rc_number", request.getId_number());
            body.put("owner_name", request.getOwner_name() != null ? request.getOwner_name() : "");
            // Consent is mandatory for many verification APIs; default to "y" or user provided
            body.put("consent", request.getConsent() != null ? request.getConsent() : "y");

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("OnGrid RC verification successful");
                Map<String, Object> rawData = (Map<String, Object>) response.getBody();
                
                // OnGrid usually returns the data in a 'data' field or directly.
                // We'll normalize it to match the expected format: { "success": true, "data": { ... } }
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                
                Map<String, Object> data = rawData.containsKey("data") ? (Map<String, Object>) rawData.get("data") : rawData;
                
                // Map OnGrid fields to the format expected by our application
                Map<String, Object> mappedData = new HashMap<>();
                mappedData.put("rc_number", data.getOrDefault("registration_number", data.get("rc_number")));
                mappedData.put("owner_name", data.get("owner_name"));
                mappedData.put("vehicle_model", data.getOrDefault("model_name", data.getOrDefault("model", data.get("vehicle_model"))));
                mappedData.put("fuel_type", data.get("fuel_type"));
                mappedData.put("registration_date", data.get("registration_date"));
                mappedData.put("fit_up_to", data.getOrDefault("fitness_upto", data.get("fit_up_to")));
                mappedData.put("insurance_company", data.get("insurance_company"));
                mappedData.put("insurance_policy_number", data.get("insurance_policy_number"));
                mappedData.put("insurance_upto", data.getOrDefault("insurance_expiry_date", data.get("insurance_upto")));
                mappedData.put("vehicle_chasi_number", data.getOrDefault("chassis_number", data.get("vehicle_chasi_number")));
                mappedData.put("vehicle_engine_number", data.getOrDefault("engine_number", data.get("vehicle_engine_number")));
                mappedData.put("rc_status", data.getOrDefault("status", data.get("rc_status")));
                mappedData.put("challan_details", data.get("challan_details"));
                mappedData.put("other_details", data.get("other_details"));

                result.put("data", mappedData);
                return result;
            }
            return createErrorResponse("OnGrid RC API returned status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("HTTP error calling OnGrid RC API: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("OnGrid RC API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception calling OnGrid RC API", e);
            return createErrorResponse("OnGrid RC API exception: " + e.getMessage());
        }
    }

    private Map<String, Object> createErrorResponse(String msg) {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", msg);
        return err;
    }

    private Map<String, Object> mockRcResponse(String rcNumber) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message_code", "success");
        Map<String, Object> data = new HashMap<>();
        data.put("rc_number", rcNumber != null ? rcNumber : "MH14MH2542");
        data.put("owner_name", "RAHUL SHARMA (ONGRID-MOCK)");
        data.put("vehicle_model", "MARUTI SUZUKI INDIA LTD / SWIFT VXI");
        data.put("fuel_type", "PETROL");
        data.put("registration_date", "2023-05-15");
        data.put("fit_up_to", "2038-05-14");
        data.put("insurance_company", "HDFC ERGO GENERAL INSURANCE CO. LTD.");
        data.put("insurance_policy_number", "8899/2023/123456");
        data.put("insurance_upto", "2024-05-14");
        data.put("vehicle_chasi_number", "MA3BNC62SSEA42874");
        data.put("vehicle_engine_number", "K15CN9792482");
        data.put("rc_status", "ACTIVE");
        data.put("challan_details", "0 Pending Challans");
        data.put("other_details", "Record verified against VAHAN Registry (ONGRID-MOCK)");
        r.put("data", data);
        return r;
    }
}
