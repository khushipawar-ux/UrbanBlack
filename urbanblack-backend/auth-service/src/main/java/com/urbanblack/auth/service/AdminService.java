package com.urbanblack.auth.service;

import com.urbanblack.auth.dto.AdminLoginRequest;
import com.urbanblack.auth.dto.AuthResponse;
import com.urbanblack.auth.exception.InvalidCredentialsException;
import com.urbanblack.auth.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    // Hardcoded Admin Credentials
    private final String ADMIN_EMAIL = "admin@urbanblack.com";
    
    // BCrypt hash for "Admin@123"
    private final String ADMIN_PASSWORD_HASH = "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgNoT7GqiclUAloUvU3.H6v54qC.";

    public AuthResponse login(AdminLoginRequest request) {
        // Verify Email
        if (!ADMIN_EMAIL.equalsIgnoreCase(request.getEmail())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Verify Password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), ADMIN_PASSWORD_HASH)) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Generate JWT Token
        String token = jwtUtils.generateToken(ADMIN_EMAIL, "ADMIN");

        return AuthResponse.builder()
                .token(token)
                .email(ADMIN_EMAIL)
                .role("ADMIN")
                .build();
    }
}
