package com.urbanblack.fleetservice.controller;

import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.fleetservice.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.urbanblack.fleetservice.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
@Validated
public class VehicleController {

    private final VehicleService vehicleService;

    // ===============================
    // 1️⃣ Get Assigned Vehicle
    // ===============================
    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<VehicleResponse>> getAssignedVehicle(
            @RequestHeader("X-Driver-Id") @NotBlank(message = "X-Driver-Id header is required") String driverId) {
        
        return vehicleService.getAssignedVehicle(driverId)
                .map(vehicle -> ResponseEntity.ok(ApiResponse.<VehicleResponse>builder()
                        .success(true)
                        .data(vehicle)
                        .build()))
                .orElse(ResponseEntity.ok(ApiResponse.<VehicleResponse>builder()
                        .success(false)
                        .message("No assigned vehicle found")
                        .build()));
    }

    // ===============================
    // 1️⃣.1️⃣ Get All Vehicles
    // ===============================
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAllVehicles() {
        List<VehicleResponse> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(ApiResponse.<List<VehicleResponse>>builder()
                .success(true)
                .data(vehicles)
                .message("All vehicles retrieved successfully")
                .build());
    }

    // ===============================
    // 2️⃣ Take Vehicle
    // ===============================
    @PostMapping("/take")
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> takeVehicle(
            @RequestHeader("X-Driver-Id") @NotBlank(message = "X-Driver-Id header is required") String driverId,
            @Valid @RequestBody TakeVehicleRequest request) {
        
        VehicleAssignmentResponse response = vehicleService.takeVehicle(driverId, request);
        
        return ResponseEntity.ok(ApiResponse.<VehicleAssignmentResponse>builder()
                .success(true)
                .data(response)
                .message("Vehicle taken successfully")
                .build());
    }

    // ===============================
    // 3️⃣ Return Vehicle
    // ===============================
    @PostMapping("/return")
    public ResponseEntity<ApiResponse<ReturnVehicleResponse>> returnVehicle(
            @RequestHeader("X-Driver-Id") @NotBlank(message = "X-Driver-Id header is required") String driverId,
            @Valid @RequestBody ReturnVehicleRequest request) {
        
        ReturnVehicleResponse response = vehicleService.returnVehicle(driverId, request);
        
        return ResponseEntity.ok(ApiResponse.<ReturnVehicleResponse>builder()
                .success(true)
                .data(response)
                .message("Vehicle returned successfully")
                .build());
    }

    // ===============================
    // 4️⃣ Admin Assign Vehicle (Bridging from Traffic Operations)
    // ===============================
    @PostMapping("/admin/assign")
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> adminAssignVehicle(
            @Valid @RequestBody AdminAssignVehicleRequest request) {
        
        VehicleAssignmentResponse response = vehicleService.adminAssignVehicle(request);
        
        return ResponseEntity.ok(ApiResponse.<VehicleAssignmentResponse>builder()
                .success(true)
                .data(response)
                .message("Vehicle assigned successfully by admin")
                .build());
    }
}
