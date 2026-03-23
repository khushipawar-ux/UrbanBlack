package com.urbanblack.driverservice.security;

import com.urbanblack.driverservice.entity.Driver;
import com.urbanblack.driverservice.repository.DriverRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Validates JWTs issued by auth-service (common auth).
 *
 * Flow:
 * 1. Extract Bearer token from Authorization header.
 * 2. Validate signature using shared jwt.secret.
 * 3. Check role claim == DRIVER.
 * 4. Look up Driver profile in driver-service DB by email.
 * 5. Set driverId + email as request attributes for controllers.
 * 6. Populate SecurityContext with ROLE_DRIVER authority.
 */
@Component
@RequiredArgsConstructor
public class DriverJwtFilter extends OncePerRequestFilter {

    private final DriverJwtUtils jwtUtils;
    private final DriverRepository driverRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator")
                || path.startsWith("/api/driver/profile/summary/")
                || path.startsWith("/admin/driver/attendance");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtils.isTokenValid(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("JWT token has expired or is invalid");
                return;
            }

            // Enforce DRIVER role — tokens issued for USER or ADMIN are rejected
            String role = jwtUtils.extractRole(token);
            if (!"DRIVER".equals(role)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Access denied: DRIVER role required");
                return;
            }

            String email = jwtUtils.extractEmail(token);

            // Look up driver profile by email to get the driver-service driverId
            Driver driver = driverRepository.findByEmail(email).orElse(null);

            if (driver == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Driver profile not found for email: " + email
                        + ". Please contact admin to complete onboarding.");
                return;
            }

            if (!driver.isActive()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Driver account is inactive. Please contact admin.");
                return;
            }

            // Expose driverId and email to all downstream controllers via request
            // attributes
            request.setAttribute("driverId", driver.getId());
            request.setAttribute("email", email);

            // Populate Spring SecurityContext
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_DRIVER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("JWT processing error: " + e.getMessage());
        }
    }
}