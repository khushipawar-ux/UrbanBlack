package EmployeeDetails_Service.EmployeeDetails_Service.Controller;

import EmployeeDetails_Service.EmployeeDetails_Service.Service.OTPService;
import EmployeeDetails_Service.EmployeeDetails_Service.dto.OtpSendRequest;
import EmployeeDetails_Service.EmployeeDetails_Service.dto.OtpSendResponse;
import EmployeeDetails_Service.EmployeeDetails_Service.dto.OtpVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OTP Controller
 *
 * Provides separate OTP send + verify endpoints for:
 *   - Aadhaar Card     → /otp/aadhaar/send  + /otp/aadhaar/verify
 *   - Driving License  → /otp/license/send  + /otp/license/verify
 *   - Bank Account     → /otp/bank/send     + /otp/bank/verify
 *
 * NOTE (Development Mode): OTPs are printed to the backend console.
 * Example log:
 *   ============================================================
 *   [AADHAAR OTP] Identifier : 123456789012
 *   [AADHAAR OTP] Code       : 482931
 *   ============================================================
 */
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Tag(
    name = "OTP Verification",
    description = "Separate OTP endpoints for Aadhaar, Driving License, and Bank Account verification"
)
public class OTPController {

    private final OTPService otpService;

    // ──────────────────────────────────────────────────────────────────────────
    //  AADHAAR OTP
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/aadhaar/send")
    @Operation(
        summary = "Send Aadhaar OTP",
        description = "Generates and sends a 6-digit OTP for Aadhaar card verification. " +
                      "The OTP is printed to the backend console (dev mode)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP generated successfully",
            content = @Content(schema = @Schema(implementation = OtpSendResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "message": "OTP sent successfully. Check backend console.",
                      "documentType": "AADHAAR",
                      "maskedIdentifier": "****9012"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Aadhaar number is required")
    })
    public ResponseEntity<OtpSendResponse> sendAadhaarOTP(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Aadhaar number to send OTP for",
                content = @Content(schema = @Schema(implementation = OtpSendRequest.class),
                    examples = @ExampleObject(value = "{\"identifier\": \"123456789012\"}")))
            @RequestBody OtpSendRequest request) {

        if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String otp = otpService.generateAadhaarOTP(request.getIdentifier());

        OtpSendResponse response = new OtpSendResponse(
            "OTP sent successfully.",
            "AADHAAR",
            maskIdentifier(request.getIdentifier()),
            otp
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/aadhaar/verify")
    @Operation(
        summary = "Verify Aadhaar OTP",
        description = "Verifies the OTP entered for Aadhaar card. " +
                      "Returns true if valid. The OTP is consumed (single-use)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns true if OTP is valid, false otherwise",
            content = @Content(schema = @Schema(implementation = Boolean.class),
                examples = @ExampleObject(value = "true"))),
        @ApiResponse(responseCode = "400", description = "Missing identifier or OTP")
    })
    public ResponseEntity<Boolean> verifyAadhaarOTP(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Aadhaar number and OTP to verify",
                content = @Content(schema = @Schema(implementation = OtpVerifyRequest.class),
                    examples = @ExampleObject(value = "{\"identifier\": \"123456789012\", \"otp\": \"482931\"}")))
            @RequestBody OtpVerifyRequest request) {

        if (request.getIdentifier() == null || request.getOtp() == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean isValid = otpService.verifyAadhaarOTP(request.getIdentifier(), request.getOtp());
        return ResponseEntity.ok(isValid);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  DRIVING LICENSE OTP
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/license/send")
    @Operation(
        summary = "Send Driving License OTP",
        description = "Generates and sends a 6-digit OTP for Driving License verification. " +
                      "The OTP is printed to the backend console (dev mode)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP generated successfully",
            content = @Content(schema = @Schema(implementation = OtpSendResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "message": "OTP sent successfully. Check backend console.",
                      "documentType": "LICENSE",
                      "maskedIdentifier": "****3333"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "License number is required")
    })
    public ResponseEntity<OtpSendResponse> sendLicenseOTP(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "License number to send OTP for",
                content = @Content(schema = @Schema(implementation = OtpSendRequest.class),
                    examples = @ExampleObject(value = "{\"identifier\": \"MH12345678\"}")))
            @RequestBody OtpSendRequest request) {

        if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String otp = otpService.generateLicenseOTP(request.getIdentifier());

        OtpSendResponse response = new OtpSendResponse(
            "OTP sent successfully.",
            "LICENSE",
            maskIdentifier(request.getIdentifier()),
            otp
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/license/verify")
    @Operation(
        summary = "Verify Driving License OTP",
        description = "Verifies the OTP entered for Driving License. " +
                      "Returns true if valid. The OTP is consumed (single-use)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns true if OTP is valid, false otherwise",
            content = @Content(schema = @Schema(implementation = Boolean.class),
                examples = @ExampleObject(value = "true"))),
        @ApiResponse(responseCode = "400", description = "Missing identifier or OTP")
    })
    public ResponseEntity<Boolean> verifyLicenseOTP(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "License number and OTP to verify",
                content = @Content(schema = @Schema(implementation = OtpVerifyRequest.class),
                    examples = @ExampleObject(value = "{\"identifier\": \"MH12345678\", \"otp\": \"382719\"}")))
            @RequestBody OtpVerifyRequest request) {

        if (request.getIdentifier() == null || request.getOtp() == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean isValid = otpService.verifyLicenseOTP(request.getIdentifier(), request.getOtp());
        return ResponseEntity.ok(isValid);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  BANK ACCOUNT OTP
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/bank/send")
    @Operation(
        summary = "Send Bank Account OTP",
        description = "Generates and sends a 6-digit OTP for Bank Account verification. " +
                      "The OTP is printed to the backend console (dev mode)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP generated successfully",
            content = @Content(schema = @Schema(implementation = OtpSendResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "message": "OTP sent successfully. Check backend console.",
                      "documentType": "BANK",
                      "maskedIdentifier": "****5678"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Account number is required")
    })
    public ResponseEntity<OtpSendResponse> sendBankOTP(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Bank account number to send OTP for",
                content = @Content(schema = @Schema(implementation = OtpSendRequest.class),
                    examples = @ExampleObject(value = "{\"identifier\": \"9876543210\"}")))
            @RequestBody OtpSendRequest request) {

        if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String otp = otpService.generateBankOTP(request.getIdentifier());

        OtpSendResponse response = new OtpSendResponse(
            "OTP sent successfully.",
            "BANK",
            maskIdentifier(request.getIdentifier()),
            otp
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bank/verify")
    @Operation(
        summary = "Verify Bank Account OTP",
        description = "Verifies the OTP entered for Bank Account. " +
                      "Returns true if valid. The OTP is consumed (single-use)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns true if OTP is valid, false otherwise",
            content = @Content(schema = @Schema(implementation = Boolean.class),
                examples = @ExampleObject(value = "true"))),
        @ApiResponse(responseCode = "400", description = "Missing identifier or OTP")
    })
    public ResponseEntity<Boolean> verifyBankOTP(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Bank account number and OTP to verify",
                content = @Content(schema = @Schema(implementation = OtpVerifyRequest.class),
                    examples = @ExampleObject(value = "{\"identifier\": \"9876543210\", \"otp\": \"193847\"}")))
            @RequestBody OtpVerifyRequest request) {

        if (request.getIdentifier() == null || request.getOtp() == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean isValid = otpService.verifyBankOTP(request.getIdentifier(), request.getOtp());
        return ResponseEntity.ok(isValid);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GENERIC (Legacy - kept for backward compatibility)
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/send")
    @Operation(
        summary = "[Legacy] Send Generic OTP",
        description = "Generic OTP endpoint (kept for backward compatibility). " +
                      "Use the document-specific endpoints instead: /aadhaar/send, /license/send, /bank/send"
    )
    @Deprecated
    public ResponseEntity<String> sendGenericOTP(@RequestBody OtpSendRequest request) {
        otpService.generateOTP(request.getIdentifier());
        return ResponseEntity.ok("OTP sent. Check backend console.");
    }

    @PostMapping("/verify")
    @Operation(
        summary = "[Legacy] Verify Generic OTP",
        description = "Generic OTP verify endpoint (kept for backward compatibility). " +
                      "Use the document-specific endpoints instead: /aadhaar/verify, /license/verify, /bank/verify"
    )
    @Deprecated
    public ResponseEntity<Boolean> verifyGenericOTP(@RequestBody OtpVerifyRequest request) {
        boolean isValid = otpService.verifyOTP(request.getIdentifier(), request.getOtp());
        return ResponseEntity.ok(isValid);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Utility
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Masks all but the last 4 characters of an identifier for safe display.
     * Example: "123456789012" → "****9012"
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 4) return "****";
        String last4 = identifier.substring(identifier.length() - 4);
        return "****" + last4;
    }
}
