package EmployeeDetails_Service.EmployeeDetails_Service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for the quickKyc-service.
 * All KYC API calls are routed through the quickKyc-service,
 * which holds the actual QuickEKYC API key securely.
 */
@FeignClient(name = "quickKyc-service", url = "${quickkyc-service.url:http://quickkyc-service:8087}")
public interface QuickKycClient {

    /**
     * Step 1 – Generate Aadhaar OTP.
     * Request: { "id_number": "123456789012" }
     * Response includes "client_id" needed for step 2.
     */
    @PostMapping("/internal/api/quickkyc/aadhaar/generate-otp")
    Map<String, Object> generateAadhaarOtp(@RequestBody Map<String, String> request);

    /**
     * Step 2 – Verify Aadhaar OTP.
     * Request: { "request_id": "<client_id from step 1>", "otp": "123456" }
     * Response: Aadhaar holder details on success.
     */
    @PostMapping("/internal/api/quickkyc/aadhaar/verify-otp")
    Map<String, Object> verifyAadhaarOtp(@RequestBody Map<String, String> request);

    /**
     * Driving License – one-shot verification (no OTP).
     * Request: { "id_number": "MH1234...", "dob": "YYYY-MM-DD" }
     * Response: DL holder details on success.
     */
    @PostMapping("/internal/api/quickkyc/driving-license")
    Map<String, Object> verifyDrivingLicense(@RequestBody Map<String, String> request);
    
    /**
     * Bank Account – one-shot Penny Drop verification.
     * Request: { "account_number": "...", "ifsc_code": "..." }
     */
    @PostMapping("/internal/api/quickkyc/bank-verification")
    Map<String, Object> verifyBankAccount(@RequestBody Map<String, String> request);
}
