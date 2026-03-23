package com.urbanblack.auth.controller;

import com.urbanblack.auth.dto.AuthResponse;
import com.urbanblack.auth.dto.LoginRequest;
import com.urbanblack.auth.dto.RegisterRequest;
import com.urbanblack.auth.service.AuthService;
import com.urbanblack.common.dto.response.*;
import com.urbanblack.common.dto.request.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> register(
            @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/admin/onboard-driver")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> onboardDriver(
            @RequestBody com.urbanblack.common.dto.request.UserRegistrationRequest request,
            @RequestHeader(value = "X-Role", required = false) String role) { // Assuming Gateway passes role

        if (role == null || !role.equals("ADMIN")) {
            // In a real scenario, Gateway handles this or SecurityContext holds it.
            // For now, we trust the header from Gateway or just check it.
             return ResponseEntity.status(403).body(
                     ApiResponse.<SimpleMessageResponse>builder()
                             .success(false)
                             .message("Access Denied: Admin role required")
                             .build()
             );
        }
        
        return ResponseEntity.ok(authService.onboardDriver(request));
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseData>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/driverlogin")
    public ResponseEntity<ApiResponse<LoginResponseData>> driverLogin(@RequestBody com.urbanblack.auth.dto.DriverLoginRequest request) {
        return ResponseEntity.ok(authService.driverLogin(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> logout(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.logout(request.getRefreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseData>> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> changePassword(
            @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> forgotPassword(
            @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request.getEmailOrPhone()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponseData>> verifyOtp(
            @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> sendOtp(
            @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> resendOtp(
            @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request.getEmailOrPhone()));
    }
}
