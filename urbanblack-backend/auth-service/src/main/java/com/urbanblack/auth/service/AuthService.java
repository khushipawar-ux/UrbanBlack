package com.urbanblack.auth.service;

import com.urbanblack.auth.dto.AuthResponse;
import com.urbanblack.auth.dto.LoginRequest;
import com.urbanblack.auth.dto.RegisterRequest;
import com.urbanblack.auth.entity.AuthUser;
import com.urbanblack.auth.repository.UserAuthRepository;
import com.urbanblack.auth.security.JwtService;
import com.urbanblack.common.dto.response.*;
import com.urbanblack.common.dto.request.*;
import com.urbanblack.auth.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import com.urbanblack.auth.entity.Otp;
import com.urbanblack.auth.repository.OtpRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAuthRepository userAuthRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final OtpRepository otpRepository;

    public ApiResponse<SimpleMessageResponse> register(RegisterRequest request) {

        if (userAuthRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        AuthUser user = AuthUser.builder()
                .name(request.getName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(com.urbanblack.auth.entity.Role.USER) // Enforce USER role
                .enabled(true)
                .build();

        userAuthRepository.save(user);
        return ApiResponse.success("User registered successfully", new SimpleMessageResponse("User registered successfully"));
    }

    public ApiResponse<LoginResponseData> login(LoginRequest request) {
        AuthUser user = userAuthRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ApiResponse.success(LoginResponseData.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build());
    }

    public ApiResponse<LoginResponseData> driverLogin(com.urbanblack.auth.dto.DriverLoginRequest request) {
        String input = request.getUsername();
        System.out.println(">>> Received driverLogin request for: " + input);

        // Try exact match first
        AuthUser user = userAuthRepository.findByEmail(input)
                .orElse(null);

        // If not found, try partial match if input doesn't contain @
        if (user == null && !input.contains("@")) {
            System.out.println(">>> Driver account not found by exact match. Trying partial match...");
            user = userAuthRepository.findAll().stream()
                    .filter(u -> u.getEmail().startsWith(input))
                    .findFirst()
                    .orElseThrow(() -> new InvalidCredentialsException("Driver account not found with: " + input));
        } else if (user == null) {
            throw new InvalidCredentialsException("Driver account not found with full email: " + input);
        }

        if (user.getRole() != com.urbanblack.auth.entity.Role.DRIVER) {
            System.out.println(">>> ROLE MISMATCH for '" + input + "'. Expected: DRIVER, Found: " + user.getRole());
            throw new RuntimeException("Unauthorized: Not a driver account");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials for: " + input);
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        System.out.println(">>> Successfully logged in driver: " + user.getEmail());

        return ApiResponse.success(LoginResponseData.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .username(user.getEmail()) // Mirroring the email to the username
                .role(user.getRole().name())
                .build());
    }

    public ApiResponse<SimpleMessageResponse> onboardDriver(com.urbanblack.common.dto.request.UserRegistrationRequest request) {

        if (userAuthRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Employee account already exists in Auth Service");
        }

        // Map incoming role string to Auth Role
        com.urbanblack.auth.entity.Role authRole = com.urbanblack.auth.entity.Role.DRIVER;
        if (request.getRole() != null && !request.getRole().equalsIgnoreCase("DRIVER")) {
            authRole = com.urbanblack.auth.entity.Role.EMPLOYEE;
        }

        AuthUser user = AuthUser.builder()
                .name(request.getName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(authRole)
                .enabled(true)
                .build();

        userAuthRepository.save(user);

        return ApiResponse.<SimpleMessageResponse>builder()
                .success(true)
                .message("Employee onboarded to Auth Service successfully with role: " + authRole)
                .build();
    }


    public ApiResponse<SimpleMessageResponse> logout(String refreshToken) {
        return ApiResponse.success("Logged out successfully", new SimpleMessageResponse("Logged out successfully"));
    }

    public ApiResponse<LoginResponseData> refresh(String refreshToken) {
        // Implementation logic...
        return ApiResponse.success(new LoginResponseData());
    }

    public ApiResponse<SimpleMessageResponse> changePassword(ChangePasswordRequest request) {
        return ApiResponse.success("Password changed successfully", new SimpleMessageResponse("Password changed successfully"));
    }

    public ApiResponse<SimpleMessageResponse> forgotPassword(String emailOrPhone) {
        // For mobile-based forgot password
        if (emailOrPhone != null && !emailOrPhone.contains("@")) {
            OtpRequest request = new OtpRequest();
            request.setEmailOrPhone(emailOrPhone);
            return sendOtp(request);
        }
        return ApiResponse.success("If an account exists, instructions will be sent to your email", new SimpleMessageResponse("Email functionality not implemented yet"));
    }

    public ApiResponse<LoginResponseData> verifyOtp(VerifyOtpRequest request) {
        String mobile = request.getEmailOrPhone();
        String enteredOtp = request.getOtp();

        Otp otpEntity = otpRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("OTP not found for this mobile number"));

        if (otpEntity.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otpEntity.getOtpValue().equals(enteredOtp)) {
            throw new RuntimeException("Invalid OTP");
        }
        
        // OTP is valid. Clear it.
        otpRepository.delete(otpEntity);

        // Check if user exists or create new one
        AuthUser user = userAuthRepository.findByMobile(mobile)
                .orElseGet(() -> {
                    AuthUser newUser = AuthUser.builder()
                            .mobile(mobile)
                            .role(com.urbanblack.auth.entity.Role.USER)
                            .enabled(true)
                            .build();
                    return userAuthRepository.save(newUser);
                });

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ApiResponse.success(LoginResponseData.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail() != null ? user.getEmail() : "")
                .role(user.getRole().name())
                .build());
    }

    public ApiResponse<SimpleMessageResponse> resetPassword(ResetPasswordRequest request) {
        return ApiResponse.success("Password reset successfully", new SimpleMessageResponse("Password reset successfully"));
    }

    public ApiResponse<SimpleMessageResponse> sendOtp(OtpRequest request) {
        String mobile = request.getEmailOrPhone(); // Client sends mobile here

        if (mobile == null || mobile.isBlank()) {
            throw new RuntimeException("Mobile number is required");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(1_000_000));

        Otp otpEntity = otpRepository.findByMobileNumber(mobile)
                .orElse(new Otp());
        
        otpEntity.setMobileNumber(mobile);
        otpEntity.setOtpValue(otp);
        otpEntity.setExpirationTime(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(otpEntity);

        // Send OTP via SMS provider (BestSMS)
        String mobileForSms = normalizeMobileForSms(mobile);
        try {
            otpService.sendOtpSms(mobileForSms, otp);
        } catch (RuntimeException ex) {
            otpRepository.delete(otpEntity);
            throw ex;
        }

        return ApiResponse.success("OTP sent successfully", new SimpleMessageResponse("OTP sent successfully"));
    }

    public ApiResponse<SimpleMessageResponse> resendOtp(String emailOrPhone) {
        OtpRequest request = new OtpRequest();
        request.setEmailOrPhone(emailOrPhone);
        return sendOtp(request);
    }

    private String normalizeMobileForSms(String mobile) {
        if (mobile == null) return null;
        String normalized = mobile.replaceAll("[^0-9]", "");
        if (normalized.startsWith("91") && normalized.length() == 12) {
            return normalized.substring(2);
        }
        if (normalized.length() == 10) {
            return normalized;
        }
        return normalized;
    }

}
