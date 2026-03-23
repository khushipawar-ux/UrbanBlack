package com.urban.quickkycservice.controller;

import com.urban.quickkycservice.dto.*;
import com.urban.quickkycservice.service.SurePassService;
import com.urban.quickkycservice.service.OnGridService;
import com.urban.quickkycservice.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal proxy controller — all QuickEKYC API calls flow through here.
 * Never exposes the API key to the frontend; it is injected server-side.
 *
 * Endpoints:
 *   POST /internal/api/quickkyc/aadhaar/generate-otp   — step 1: send OTP
 *   POST /internal/api/quickkyc/aadhaar/verify-otp     — step 2: verify OTP
 *   POST /internal/api/quickkyc/driving-license        — one-shot DL verify
 *   POST /internal/api/quickkyc/rc-advance             — RC verify (existing)
 *   POST /internal/api/quickkyc/bank-verification      — bank verify (existing)
 */
@RestController
@RequestMapping("/internal/api/quickkyc")
@Slf4j
@RequiredArgsConstructor
public class QuickKycProxyController {

    private final SurePassService surePassService;
    private final OnGridService onGridService;
    private final SmsService smsService;

    // ─────────────────────────────────────────────────────────────────────────
    //  AADHAAR  OTP – STEP 1: GENERATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Triggers OTP to be sent to the mobile number linked with the provided Aadhaar.
     * Body: { "id_number": "123456789012" }
     * On success, the response contains a "client_id" (or "transaction_id") that
     * the backend must keep and pass to the verify endpoint.
     */
    @PostMapping("/aadhaar/generate-otp")
    public ResponseEntity<Map<String, Object>> generateAadhaarOtp(@RequestBody OtpRequest request) {
        log.info("Aadhaar OTP generate request for: {}", mask(request.getId_number()));

        if (request.getId_number() == null || request.getId_number().isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("Aadhaar number is required"));
        }

        Map<String, Object> result = surePassService.generateAadhaarOtp(request.getId_number());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AADHAAR  OTP – STEP 2: VERIFY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the OTP entered by the admin.
     * Body: { "client_id": "...", "otp": "123456" }
     * On success, returns full Aadhaar details.
     */
    @PostMapping("/aadhaar/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyAadhaarOtp(@RequestBody OtpVerificationRequest request) {
        log.info("Aadhaar OTP verify request for clientId: {}", request.getRequest_id());

        if (request.getOtp() == null || request.getOtp().isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("OTP is required"));
        }
        // request_id is used as client_id here
        String clientId = request.getRequest_id();
        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("client_id (request_id) is required"));
        }

        Map<String, Object> result = surePassService.verifyAadhaarOtp(clientId, request.getOtp());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DRIVING LICENSE – DIRECT VERIFICATION (no OTP step)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies a Driving License directly via QuickEKYC.
     * Body: { "id_number": "MH1234...", "dob": "1990-01-01" }  (dob optional)
     * On success, returns DL holder details — no separate OTP step needed.
     */
    @PostMapping("/driving-license")
    public ResponseEntity<Map<String, Object>> verifyDrivingLicense(@RequestBody DrivingLicenseRequest request) {
        log.info("DL verification request for: {}", request.getId_number());

        if (request.getId_number() == null || request.getId_number().isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("License number is required"));
        }

        Map<String, Object> result = surePassService.verifyDrivingLicense(request.getId_number(), request.getDob());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RC – EXISTING
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/rc-advance")
    public ResponseEntity<Map<String, Object>> rcAdvance(@RequestBody RcAdvanceRequest request) {
        log.info("RC Advance request (OnGrid) for: {}", request.getId_number());

        if (request == null || request.getId_number() == null) {
            return ResponseEntity.badRequest().body(errorBody("RC number is required"));
        }

        return ResponseEntity.ok(onGridService.verifyRc(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BANK VERIFICATION – existing (mock)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/bank-verification")
    public ResponseEntity<Map<String, Object>> bankVerification(@RequestBody BankVerificationRequest request) {
        log.info("Bank verification for Account: {}, IFSC: {}", 
                mask(request.getAccount_number()), request.getIfsc_code());

        if (request.getAccount_number() == null || request.getAccount_number().isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("Account number is required"));
        }
        if (request.getIfsc_code() == null || request.getIfsc_code().isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("IFSC code is required"));
        }

        Map<String, Object> result = surePassService.verifyBankAccount(request.getAccount_number(), request.getIfsc_code());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SMS TEST
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/test-sms")
    public ResponseEntity<Map<String, Object>> testSms(@RequestBody SmsTestRequest testRequest) {
        String mobile = testRequest.getMobile();
        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
        log.info("Testing SMS for mobile: {} with OTP: {}", mobile, otp);
        boolean sent = smsService.sendOtpSms(mobile, otp);

        Map<String, Object> response = new HashMap<>();
        response.put("success", sent);
        response.put("message", sent ? "Test SMS sent successfully" : "Failed to send test SMS");
        response.put("mobile", mobile);
        response.put("generated_otp", otp);
        response.put("provider", "BestSMS");
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITY
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", message);
        return err;
    }

    private String mask(String s) {
        return (s != null && s.length() > 4) ? "XXXX" + s.substring(s.length() - 4) : "XXXX";
    }
}