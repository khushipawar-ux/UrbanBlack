package com.urban.quickkycservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurePassService {

    private final RestTemplate restTemplate;

    @Value("${quickkyc.api.url:https://api.quickekyc.com/api/v1}")
    private String baseUrl;

    @Value("${quickkyc.api.key:}")
    private String apiToken;

    @Value("${quickkyc.api.mock-mode:false}")
    private boolean mockMode;

    // ─────────────────────────────────────────────────────────────────────────
    //  AADHAAR OTP – SEND
    //  QuickEKYC endpoint: POST /aadhaar-v2/generate-otp
    //  Body: { key, id_number }
    //  Success response contains a "client_id" used in the verify step.
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> generateAadhaarOtp(String aadhaarNumber) {
        if (mockMode) {
            log.info("[MOCK] Generating Aadhaar OTP for: {}", mask(aadhaarNumber));
            return mockAadhaarOtpSendResponse(aadhaarNumber);
        }

        String url = baseUrl + "/aadhaar-v2/generate-otp";
        log.info("Calling QuickEKYC generate-otp: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("key", apiToken);
            body.put("id_number", aadhaarNumber);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Aadhaar OTP generation successful for: {}", mask(aadhaarNumber));
                return (Map<String, Object>) response.getBody();
            }
            return createErrorResponse("Aadhaar OTP API returned status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("HTTP error calling generate-otp: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("Aadhaar OTP API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception calling generate-otp", e);
            return createErrorResponse("Aadhaar OTP API exception: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AADHAAR OTP – VERIFY
    //  QuickEKYC endpoint: POST /aadhaar-v2/submit-otp
    //  Body: { key, client_id, otp }
    //  client_id comes from the generate-otp response.
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> verifyAadhaarOtp(String clientId, String otp) {
        if (mockMode) {
            log.info("[MOCK] Verifying Aadhaar OTP for clientId: {}", clientId);
            return mockAadhaarOtpVerifyResponse(otp);
        }

        String url = baseUrl + "/aadhaar-v2/submit-otp";
        log.info("Calling QuickEKYC submit-otp: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("key", apiToken);
            body.put("request_id", clientId);
            body.put("otp", otp);


            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Aadhaar OTP verification successful");
                return (Map<String, Object>) response.getBody();
            }
            return createErrorResponse("Aadhaar OTP verify returned status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("HTTP error calling submit-otp: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("Aadhaar OTP verify error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception calling submit-otp", e);
            return createErrorResponse("Aadhaar OTP verify exception: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DRIVING LICENSE VERIFICATION
    //  QuickEKYC endpoint: POST /driving-license/driving-license
    //  Body: { key, id_number, dob }   — dob is YYYY-MM-DD, required by API
    //  Response: DL holder details directly (no OTP step)
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> verifyDrivingLicense(String dlNumber, String dob) {
        if (mockMode) {
            log.info("[MOCK] Verifying DL: {}", mask(dlNumber));
            return mockDLVerifyResponse(dlNumber, dob);
        }

        // QuickEKYC endpoint: https://api.quickekyc.com/api/v1/driving-license/driving-license
        String url = baseUrl + "/driving-license/driving-license";
        log.info("Calling QuickEKYC DL verify for: {}", mask(dlNumber));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("key", apiToken);
            // QuickEKYC uses "id_number" for the license number
            body.put("id_number", dlNumber);
            if (dob != null && !dob.isBlank()) {
                body.put("dob", dob);
            }

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("DL verification successful for: {}", dlNumber);
                return (Map<String, Object>) response.getBody();
            }
            return createErrorResponse("DL API returned status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("HTTP error calling DL API: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("DL API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception calling DL API", e);
            return createErrorResponse("DL API exception: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BANK ACCOUNT VERIFICATION (Penny Drop)
    //  QuickEKYC endpoint: POST /bank-verification/pd
    //  Body: { key, id_number, ifsc }
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> verifyBankAccount(String accountNumber, String ifsc) {
        if (mockMode) {
            log.info("[MOCK] Verifying bank account: {} with IFSC: {}", mask(accountNumber), ifsc);
            return mockBankVerifyResponse(accountNumber, ifsc);
        }

        // QuickEKYC endpoint: https://api.quickekyc.com/api/v1/bank-verification/pd
        String url = baseUrl + "/bank-verification/pd";
        log.info("Calling QuickEKYC Bank verify for: {}", mask(accountNumber));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("key", apiToken);
            body.put("id_number", accountNumber);
            body.put("ifsc", ifsc);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Bank verification successful for: {}", mask(accountNumber));
                return (Map<String, Object>) response.getBody();
            }
            return createErrorResponse("Bank API returned status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("HTTP error calling Bank API: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("Bank API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception calling Bank API", e);
            return createErrorResponse("Bank API exception: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RC VERIFICATION (existing)
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> verifyRc(Map<String, String> request) {
        if (mockMode) {
            log.info("[MOCK] RC verification for: {}", request.get("id_number"));
            return mockRcResponse(request);
        }

        String url = baseUrl + (baseUrl.contains("quickekyc.com") ? "/rc/rc-full" : "/kyc/rc-advance");
        log.info("Calling QuickEKYC RC API: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("key", apiToken);
            body.put("id_number", request.get("id_number"));
            if (request.containsKey("chassis_number")) body.put("chassis_number", request.get("chassis_number"));
            if (request.containsKey("engine_number")) body.put("engine_number", request.get("engine_number"));

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("RC verification successful");
                return (Map<String, Object>) response.getBody();
            }
            return createErrorResponse("RC API returned status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("HTTP error calling RC API: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("RC API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception calling RC API", e);
            return createErrorResponse("RC API exception: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> createErrorResponse(String msg) {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", msg);
        return err;
    }

    private String mask(String s) {
        return (s != null && s.length() > 4) ? "XXXX" + s.substring(s.length() - 4) : "XXXX";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MOCK RESPONSES   (only used when mock-mode=true)
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> mockAadhaarOtpSendResponse(String aadhaarNumber) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message_code", "success");
        r.put("message", "OTP sent to registered mobile number.");
        r.put("request_id", "mock-req-" + UUID.randomUUID().toString().substring(0, 8));
        Map<String, Object> data = new HashMap<>();
        data.put("id_number", aadhaarNumber);
        r.put("data", data);
        return r;
    }

    private Map<String, Object> mockAadhaarOtpVerifyResponse(String otp) {
        Map<String, Object> r = new HashMap<>();
        // In mock mode, OTP "123456" always succeeds, any other fails
        if ("123456".equals(otp)) {
            r.put("success", true);
            r.put("message_code", "success");
            r.put("message", "Aadhaar verified successfully (MOCK).");
            Map<String, Object> data = new HashMap<>();
            data.put("full_name", "MOCK AADHAAR USER");
            data.put("dob", "1990-01-01");
            data.put("gender", "M");
            data.put("zip", "411041");
            data.put("address", "Mock House, Mock Street, Mock City, Maharashtra");
            data.put("mobile_hash", "xxxxxxxxxxxx");
            data.put("profile_image", "");
            r.put("data", data);
        } else {
            r.put("success", false);
            r.put("message_code", "invalid_otp");
            r.put("message", "Invalid OTP. Please try again. (MOCK: use 123456)");
        }
        return r;
    }

    private Map<String, Object> mockDLVerifyResponse(String dlNumber, String dob) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message_code", "success");
        Map<String, Object> data = new HashMap<>();
        data.put("dl_number", dlNumber);
        data.put("full_name", "MOCK DRIVER NAME");
        data.put("dob", dob);
        data.put("father_name", "MOCK FATHER NAME");
        data.put("issue_date", "2015-03-10");
        data.put("valid_upto", "2035-03-09");
        data.put("status", "ACTIVE");
        data.put("vehicle_class", "MCWG, LMV");
        data.put("blood_group", "A+");
        r.put("data", data);
        return r;
    }

    private Map<String, Object> mockBankVerifyResponse(String accountNumber, String ifsc) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message_code", "success");
        r.put("message", "Bank account verified successfully (MOCK PD).");
        Map<String, Object> data = new HashMap<>();
        data.put("id_number", accountNumber);
        data.put("ifsc", ifsc);
        data.put("full_name", "MOCK ACCOUNT HOLDER");
        data.put("bank_name", "MOCK BANK OF INDIA");
        r.put("data", data);
        return r;
    }

    private Map<String, Object> mockRcResponse(Map<String, String> req) {
        String rcNumber = req.get("id_number");
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message_code", "success");
        Map<String, Object> data = new HashMap<>();
        data.put("rc_number", rcNumber != null ? rcNumber : "MH14MH2542");
        data.put("owner_name", "RAHUL SHARMA");
        data.put("vehicle_model", "MARUTI SUZUKI INDIA LTD / SWIFT VXI");
        data.put("fuel_type", "PETROL");
        data.put("registration_date", "2023-05-15");
        data.put("fit_up_to", "2038-05-14");
        data.put("insurance_company", "HDFC ERGO GENERAL INSURANCE CO. LTD.");
        data.put("insurance_policy_number", "8899/2023/123456");
        data.put("insurance_upto", "2024-05-14");
        data.put("vehicle_chasi_number", req.getOrDefault("chassis_number", "MA3BNC62SSEA42874"));
        data.put("vehicle_engine_number", req.getOrDefault("engine_number", "K15CN9792482"));
        data.put("rc_status", "ACTIVE");
        data.put("challan_details", "0 Pending Challans");
        data.put("other_details", "Record verified against VAHAN Registry (MOCK)");
        r.put("data", data);
        return r;
    }
}
