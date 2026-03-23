package com.urbanblack.fleetservice.controller;

import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.fleetservice.dto.FuelEntryRequest;
import com.urbanblack.fleetservice.dto.FuelEntryResponse;
import com.urbanblack.fleetservice.dto.FuelHistoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import com.urbanblack.fleetservice.service.FuelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/fuel")
@RequiredArgsConstructor
@Validated
public class FuelController {

    private final FuelService fuelService;

    // ===============================
    // 1️⃣ Submit Fuel Entry
    // ===============================
    @PostMapping("/entry")
    public ResponseEntity<ApiResponse<FuelEntryResponse>> submitFuelEntry(
            @RequestHeader("X-Driver-Id") @NotBlank(message = "X-Driver-Id header is required") String driverId,
            @Valid @RequestBody FuelEntryRequest request) {
        
        FuelEntryResponse response = fuelService.submitFuelEntry(driverId, request);
        
        return ResponseEntity.ok(ApiResponse.<FuelEntryResponse>builder()
                .success(true)
                .data(response)
                .message("Fuel entry submitted for approval")
                .build());
    }

    // ===============================
    // 2️⃣ Get Fuel History
    // ===============================
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<FuelHistoryResponse>>> getFuelHistory(
            @RequestHeader("X-Driver-Id") @NotBlank(message = "X-Driver-Id header is required") String driverId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be >= 1") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be >= 1") @Max(value = 100, message = "limit must be <= 100") int limit) {
        
        List<FuelHistoryResponse> history = fuelService.getFuelHistory(driverId, page, limit);
        if (history == null) {
            history = Collections.emptyList();
        }
        
        return ResponseEntity.ok(ApiResponse.<List<FuelHistoryResponse>>builder()
                .success(true)
                .data(history)
                .build());
    }
}
