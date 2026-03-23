package com.urbanblack.driverservice.controller;

import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.driverservice.dto.DriverSummaryDto;
import com.urbanblack.driverservice.entity.Driver;
import com.urbanblack.driverservice.service.DriverService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/profile")
@RequiredArgsConstructor
public class DriverProfileController {

    private final DriverService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Driver>> getProfile(HttpServletRequest request) {
        String email = (String) request.getAttribute("email");
        return ResponseEntity.ok(service.getProfile(email));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Driver>> updateProfile(
            HttpServletRequest request,
            @RequestBody Driver updated
    ) {
        String email = (String) request.getAttribute("email");
        return ResponseEntity.ok(service.updateProfile(email, updated));
    }

    @GetMapping("/summary/{driverId}")
    public ResponseEntity<DriverSummaryDto> getDriverSummary(@PathVariable String driverId) {
        return ResponseEntity.ok(service.getDriverSummary(driverId));
    }
}