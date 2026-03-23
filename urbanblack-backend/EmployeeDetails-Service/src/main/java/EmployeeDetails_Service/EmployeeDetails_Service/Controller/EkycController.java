package EmployeeDetails_Service.EmployeeDetails_Service.Controller;

import EmployeeDetails_Service.EmployeeDetails_Service.Service.EkycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * EkycController
 *
 * REST API for the employee-onboarding eKYC verification flows.
 * The frontend calls these endpoints; actual QuickEKYC API calls stay in the backend.
 *
 * Aadhaar flow:
 *   POST /ekyc/aadhaar/send-otp    { "aadhaarNumber": "..." }
 *   POST /ekyc/aadhaar/verify-otp  { "aadhaarNumber": "...", "otp": "..." }
 *
 * Driving License flow:
 *   POST /ekyc/driving-license/verify  { "licenseNumber": "...", "dob": "YYYY-MM-DD" }
 */
@RestController
@RequestMapping("/ekyc")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "eKYC Verification", description = "Employee KYC verification endpoints (Aadhaar OTP, Driving License)")
public class EkycController {

    private final EkycService ekycService;

    // ─────────────────────────────────────────────────────────────────────────
    //  AADHAAR – STEP 1: SEND OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Triggers an OTP to the mobile linked with the Aadhaar number.
     * The admin enters the OTP received on the employee's phone.
     *
     * Request body: { "aadhaarNumber": "123456789012" }
     */
    @PostMapping("/aadhaar/send-otp")
    @Operation(
        summary = "Send Aadhaar OTP",
        description = "Calls QuickEKYC to send OTP to the mobile number registered with the given Aadhaar. " +
                      "The backend stores the returned client_id for the verification step."
    )
    public ResponseEntity<Map<String, Object>> sendAadhaarOtp(@RequestBody Map<String, String> request) {
        String aadhaarNumber = request.get("aadhaarNumber");

        if (aadhaarNumber == null || aadhaarNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "aadhaarNumber is required"));
        }
        if (aadhaarNumber.length() != 12) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Aadhaar number must be exactly 12 digits"));
        }

        log.info("EKYC send-aadhaar-otp for: XXXX{}", aadhaarNumber.substring(8));
        Map<String, Object> result = ekycService.sendAadhaarOtp(aadhaarNumber);
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AADHAAR – STEP 2: VERIFY OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the OTP entered for Aadhaar.
     *
     * Request body: { "aadhaarNumber": "123456789012", "otp": "483921" }
     * Response on success: Aadhaar holder details (name, dob, gender, address etc.)
     */
    @PostMapping("/aadhaar/verify-otp")
    @Operation(
        summary = "Verify Aadhaar OTP",
        description = "Verifies the OTP that was sent to the Aadhaar-linked mobile. " +
                      "Returns Aadhaar holder details on success."
    )
    public ResponseEntity<Map<String, Object>> verifyAadhaarOtp(@RequestBody Map<String, String> request) {
        String aadhaarNumber = request.get("aadhaarNumber");
        String otp = request.get("otp");

        if (aadhaarNumber == null || aadhaarNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "aadhaarNumber is required"));
        }
        if (otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "otp is required"));
        }

        log.info("EKYC verify-aadhaar-otp for: XXXX{}", aadhaarNumber.substring(Math.max(0, aadhaarNumber.length() - 4)));
        Map<String, Object> result = ekycService.verifyAadhaarOtp(aadhaarNumber, otp);
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DRIVING LICENSE – DIRECT VERIFY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Directly verifies a Driving License via QuickEKYC (no OTP required).
     * When the admin clicks "Send" for the License field, this endpoint is called.
     * A successful response means the license is valid — frontend marks it Verified.
     *
     * Request body: { "licenseNumber": "MH1234...", "dob": "1990-01-15" }
     */
    @PostMapping("/driving-license/verify")
    @Operation(
        summary = "Verify Driving License",
        description = "One-shot Driving License verification via QuickEKYC. " +
                      "Returns DL holder details on success. No OTP step."
    )
    public ResponseEntity<Map<String, Object>> verifyDrivingLicense(@RequestBody Map<String, String> request) {
        String licenseNumber = request.get("licenseNumber");
        String dob = request.get("dob");

        if (licenseNumber == null || licenseNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "licenseNumber is required"));
        }

        log.info("EKYC verify-dl for: {}", licenseNumber);
        Map<String, Object> result = ekycService.verifyDrivingLicense(licenseNumber, dob);
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BANK ACCOUNT – DIRECT VERIFY (Penny Drop)
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Verifies a Bank Account via QuickEKYC Penny Drop (no OTP required).
     *
     * Request body: { "accountNumber": "...", "ifsc": "..." }
     */
    @PostMapping("/bank/verify")
    @Operation(
        summary = "Verify Bank Account",
        description = "One-shot Bank Account verification via QuickEKYC Penny Drop. " +
                      "Returns account holder details on success. No OTP step."
    )
    public ResponseEntity<Map<String, Object>> verifyBankAccount(@RequestBody Map<String, String> request) {
        String accountNumber = request.get("accountNumber");
        String ifsc = request.get("ifsc");

        if (accountNumber == null || accountNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "accountNumber is required"));
        }
        if (ifsc == null || ifsc.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "ifsc is required"));
        }

        log.info("EKYC verify-bank for: {}", accountNumber);
        Map<String, Object> result = ekycService.verifyBankAccount(accountNumber, ifsc);
        return ResponseEntity.ok(result);
    }
}
