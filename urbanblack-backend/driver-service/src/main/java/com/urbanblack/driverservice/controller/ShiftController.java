package com.urbanblack.driverservice.controller;

import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.driverservice.dto.ClockInRequest;
import com.urbanblack.driverservice.dto.ClockOutRequest;
import com.urbanblack.driverservice.entity.Shift;
import com.urbanblack.driverservice.service.ShiftService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shift")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    /**
     * POST /api/shift/clock-in
     * Driver clocks in for a new shift. Requires JWT.
     */
    @PostMapping("/clock-in")
    public ResponseEntity<ApiResponse<Shift>> clockIn(
            HttpServletRequest request,
            @RequestBody(required = false) ClockInRequest clockInRequest
    ) {
        String driverId = (String) request.getAttribute("driverId");
        return ResponseEntity.ok(shiftService.clockIn(driverId, clockInRequest));
    }

    /**
     * POST /api/shift/offline
     * Driver goes on a break (pauses duration).
     * Path matches mobile app: /drivers/api/shift/offline (with StripPrefix=1)
     */
    @PostMapping("/offline")
    public ResponseEntity<ApiResponse<Shift>> goOffline(HttpServletRequest request) {
        String driverId = (String) request.getAttribute("driverId");
        return ResponseEntity.ok(shiftService.goOffline(driverId));
    }

    /**
     * POST /api/shift/online
     * Driver finishes break (resumes duration).
     * Path matches mobile app: /drivers/api/shift/online (with StripPrefix=1)
     */
    @PostMapping("/online")
    public ResponseEntity<ApiResponse<Shift>> goOnline(HttpServletRequest request) {
        String driverId = (String) request.getAttribute("driverId");
        return ResponseEntity.ok(shiftService.goOnline(driverId));
    }

    /**
     * POST /api/shift/clock-out
     * Driver completes shift.
     */
    @PostMapping("/clock-out")
    public ResponseEntity<ApiResponse<Shift>> clockOut(
            HttpServletRequest request,
            @RequestBody(required = false) ClockOutRequest clockOutRequest
    ) {
        String driverId = (String) request.getAttribute("driverId");
        return ResponseEntity.ok(shiftService.clockOut(driverId, clockOutRequest));
    }

    /**
     * GET /api/shift/status
     * Returns the currently active shift (if any).
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Shift>> getStatus(HttpServletRequest request) {
        String driverId = (String) request.getAttribute("driverId");
        return ResponseEntity.ok(shiftService.getShiftStatus(driverId));
    }
}