package EmployeeDetails_Service.EmployeeDetails_Service.Service;

import EmployeeDetails_Service.EmployeeDetails_Service.client.QuickKycClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EkycService
 *
 * Orchestrates the eKYC verification flows for the employee onboarding process:
 *
 *   Aadhaar:
 *     1. sendAadhaarOtp(aadhaarNumber)    → calls QuickKYC → gets client_id → stores in memory
 *     2. verifyAadhaarOtp(aadhaarNumber, otp) → looks up client_id → calls QuickKYC → returns result
 *
 *   Driving License (direct):
 *     verifyDrivingLicense(dlNumber, dob) → calls QuickKYC one-shot → returns holder details
 *
 * WHY: The client_id returned by QuickEKYC's generate-otp must be stored somewhere
 * between the "Send" click and the "Verify" click. We use a ConcurrentHashMap keyed
 * by aadhaarNumber for simplicity (scoped to this service instance).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EkycService {

    private final QuickKycClient quickKycClient;

    /**
     * In-memory store: aadhaarNumber → client_id from QuickEKYC.
     * This is sufficient for the face-to-face onboarding use-case because the
     * admin goes through a single "send → verify" cycle per session.
     */
    private final Map<String, String> aadhaarClientIdStore = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  AADHAAR – STEP 1: SEND OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Contacts QuickEKYC to trigger an OTP to the Aadhaar-registered mobile.
     *
     * @param aadhaarNumber 12-digit Aadhaar number
     * @return API response (includes message about OTP delivery; NOT the OTP itself)
     */
    public Map<String, Object> sendAadhaarOtp(String aadhaarNumber) {
        if (aadhaarNumber == null) return errorResponse("Aadhaar number is required");
        String cleanAadhaar = aadhaarNumber.trim();
        log.info("Initiating Aadhaar OTP for: {}", mask(cleanAadhaar));

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("id_number", cleanAadhaar);

        try {
            Map<String, Object> response = quickKycClient.generateAadhaarOtp(requestBody);
            log.info("QuickEKYC generate-otp response: {}", response);

            // Extract client_id from multiple possible locations/keys
            String clientId = extractClientId(response);
            
            // If we got a client_id, it's a success regardless of the 'success' flag
            if (clientId != null) {
                aadhaarClientIdStore.put(cleanAadhaar, clientId);
                log.info("Successfully stored client_id for aadhaar: {}", mask(cleanAadhaar));
                
                // Ensure the response has success=true so the frontend is happy
                if (!Boolean.TRUE.equals(response.get("success"))) {
                    response.put("success", true);
                }
            } else {
                log.warn("Failed to extract client_id from response for: {}", mask(cleanAadhaar));
            }

            return response;

        } catch (Exception e) {
            log.error("Exception calling generate-otp for aadhaar: {}", mask(cleanAadhaar), e);
            return errorResponse("Failed to send Aadhaar OTP: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AADHAAR – STEP 2: VERIFY OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Submits the OTP to QuickEKYC using the stored client_id.
     *
     * @param aadhaarNumber Aadhaar number (used to look up stored client_id)
     * @param otp           OTP entered by the admin
     * @return API response with Aadhaar holder details on success
     */
    public Map<String, Object> verifyAadhaarOtp(String aadhaarNumber, String otp) {
        if (aadhaarNumber == null) return errorResponse("Aadhaar number is required");
        String cleanAadhaar = aadhaarNumber.trim();
        log.info("Verifying Aadhaar OTP for: {}", mask(cleanAadhaar));

        String clientId = aadhaarClientIdStore.get(cleanAadhaar);
        if (clientId == null) {
            log.warn("No client_id found for aadhaar: {}. Map contains: {}", mask(cleanAadhaar), aadhaarClientIdStore.keySet());
            return errorResponse("OTP session expired or not initiated. Please click 'Send' again.");
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("request_id", clientId);
        requestBody.put("otp", otp);

        try {
            Map<String, Object> response = quickKycClient.verifyAadhaarOtp(requestBody);
            log.info("QuickEKYC verify-otp response: {}", response);

            // On successful verify, remove the stored client_id (single-use)
            if (Boolean.TRUE.equals(response.get("success")) || 
                "success".equalsIgnoreCase(String.valueOf(response.get("status")))) {
                
                aadhaarClientIdStore.remove(cleanAadhaar);
                log.info("Aadhaar OTP verified successfully for: {}", mask(cleanAadhaar));
                
                // Ensure success flag is set for frontend
                response.put("success", true);
            }

            return response;

        } catch (Exception e) {
            log.error("Error calling verify-otp for aadhaar: {}", mask(cleanAadhaar), e);
            return errorResponse("Failed to verify Aadhaar OTP: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DRIVING LICENSE – DIRECT VERIFY (no OTP)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the Driving License directly via QuickEKYC (no OTP step).
     *
     * @param dlNumber  Driving License number
     * @param dob       Date of birth (YYYY-MM-DD, optional)
     * @return DL holder details on success
     */
    public Map<String, Object> verifyDrivingLicense(String dlNumber, String dob) {
        log.info("Verifying DL: {}", mask(dlNumber));

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("id_number", dlNumber);
        if (dob != null && !dob.isBlank()) {
            requestBody.put("dob", dob);
        }

        try {
            Map<String, Object> response = quickKycClient.verifyDrivingLicense(requestBody);
            log.info("QuickEKYC DL verify response: {}", response);

            // Normalize success status just like Aadhaar verification
            if (Boolean.TRUE.equals(response.get("success")) || 
                "success".equalsIgnoreCase(String.valueOf(response.get("status")))) {
                
                log.info("DL verified successfully for: {}", dlNumber);
                
                // Ensure the response has success=true for the frontend
                if (!Boolean.TRUE.equals(response.get("success"))) {
                    response.put("success", true);
                }
            }

            return response;
        } catch (Exception e) {
            log.error("Error verifying DL: {}", dlNumber, e);
            return errorResponse("Failed to verify Driving License: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BANK ACCOUNT – DIRECT VERIFY (Penny Drop)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the Bank Account via QuickEKYC Penny Drop (no OTP step).
     *
     * @param accountNumber Bank account number
     * @param ifsc          IFSC code
     * @return Bank details on success
     */
    public Map<String, Object> verifyBankAccount(String accountNumber, String ifsc) {
        log.info("Verifying Bank Account: {}, IFSC: {}", mask(accountNumber), ifsc);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("account_number", accountNumber);
        requestBody.put("ifsc_code", ifsc);

        try {
            Map<String, Object> response = quickKycClient.verifyBankAccount(requestBody);
            log.info("QuickEKYC Bank verify response: {}", response);

            // Normalize success status
            if (Boolean.TRUE.equals(response.get("success")) || 
                "success".equalsIgnoreCase(String.valueOf(response.get("status")))) {
                
                log.info("Bank Account verified successfully for: {}", mask(accountNumber));
                
                if (!Boolean.TRUE.equals(response.get("success"))) {
                    response.put("success", true);
                }
            }

            return response;
        } catch (Exception e) {
            log.error("Error verifying Bank Account: {}", mask(accountNumber), e);
            return errorResponse("Failed to verify Bank Account: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String extractClientId(Map<String, Object> response) {
        // Check for common client ID keys at top level
        String[] keys = {"client_id", "request_id", "transaction_id", "reference_id"};
        for (String key : keys) {
            Object val = response.get(key);
            if (val != null) return String.valueOf(val);
        }

        // Some versions nest it inside "data"
        if (response.get("data") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            for (String key : keys) {
                Object nested = data.get(key);
                if (nested != null) return String.valueOf(nested);
            }
        }
        return null;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", message);
        return err;
    }

    private String mask(String s) {
        return (s != null && s.length() > 4) ? "XXXX" + s.substring(s.length() - 4) : "XXXX";
    }
}
