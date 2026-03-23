package com.urbanblack.driverservice.config;

import com.urbanblack.driverservice.security.DriverJwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final DriverJwtFilter driverJwtFilter;

    /**
     * Prevent Spring Boot from ALSO registering DriverJwtFilter as a raw Servlet
     * filter. Without this, it runs TWICE: once outside the Security chain (before
     * anything else) and once INSIDE the chain.
     */
    @Bean
    public FilterRegistrationBean<DriverJwtFilter> registration(DriverJwtFilter filter) {
        FilterRegistrationBean<DriverJwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/driver/auth/**").permitAll()
                        .requestMatchers("/api/driver/profile/summary/**").permitAll()
                        .requestMatchers("/admin/driver/attendance/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                // Add the JWT filter before the standard authentication filter
                .addFilterBefore(driverJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}