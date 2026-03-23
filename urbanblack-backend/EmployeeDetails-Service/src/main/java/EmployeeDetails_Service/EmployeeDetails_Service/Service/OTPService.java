package EmployeeDetails_Service.EmployeeDetails_Service.Service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP Service
 * Handles OTP generation and verification for all document types.
 * Stores OTPs in-memory with a type-scoped key (type:identifier)
 * so that Aadhaar, License, and Bank OTPs don't collide.
 */
@Service
public class OTPService {

    // Key format: "TYPE:identifier"  (e.g. "AADHAAR:123456789012")
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

    // ─── Core helpers ──────────────────────────────────────────────────────────

    private String scopedKey(String type, String identifier) {
        return type.toUpperCase() + ":" + identifier;
    }

    private String generate6DigitOtp() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private void logOtp(String type, String identifier, String otp) {
        System.out.println("============================================================");
        System.out.println("[" + type + " OTP] Identifier : " + identifier);
        System.out.println("[" + type + " OTP] Code       : " + otp);
        System.out.println("============================================================");
    }

    // ─── Generic (legacy - kept for backward compatibility) ────────────────────

    public String generateOTP(String identifier) {
        String otp = generate6DigitOtp();
        otpStore.put(identifier, otp);
        logOtp("GENERIC", identifier, otp);
        return otp;
    }

    public boolean verifyOTP(String identifier, String otp) {
        String storedOtp = otpStore.get(identifier);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(identifier);
            return true;
        }
        return false;
    }

    // ─── Aadhaar OTP ───────────────────────────────────────────────────────────

    public String generateAadhaarOTP(String aadhaarNumber) {
        String otp = generate6DigitOtp();
        otpStore.put(scopedKey("AADHAAR", aadhaarNumber), otp);
        logOtp("AADHAAR", aadhaarNumber, otp);
        return otp;
    }

    public boolean verifyAadhaarOTP(String aadhaarNumber, String otp) {
        String key = scopedKey("AADHAAR", aadhaarNumber);
        String storedOtp = otpStore.get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(key);
            return true;
        }
        return false;
    }

    // ─── Driving License OTP ───────────────────────────────────────────────────

    public String generateLicenseOTP(String licenseNumber) {
        String otp = generate6DigitOtp();
        otpStore.put(scopedKey("LICENSE", licenseNumber), otp);
        logOtp("LICENSE", licenseNumber, otp);
        return otp;
    }

    public boolean verifyLicenseOTP(String licenseNumber, String otp) {
        String key = scopedKey("LICENSE", licenseNumber);
        String storedOtp = otpStore.get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(key);
            return true;
        }
        return false;
    }

    // ─── Bank Account OTP ──────────────────────────────────────────────────────

    public String generateBankOTP(String accountNumber) {
        String otp = generate6DigitOtp();
        otpStore.put(scopedKey("BANK", accountNumber), otp);
        logOtp("BANK", accountNumber, otp);
        return otp;
    }

    public boolean verifyBankOTP(String accountNumber, String otp) {
        String key = scopedKey("BANK", accountNumber);
        String storedOtp = otpStore.get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(key);
            return true;
        }
        return false;
    }
}
