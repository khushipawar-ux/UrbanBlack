package com.urbanblack.rideservice.controller;

import com.urbanblack.rideservice.entity.Ride;
import com.urbanblack.rideservice.service.RideService;
import com.urbanblack.rideservice.service.RewardTreeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final RewardTreeService rewardTreeService;

    @PostMapping("/complete")
    public ResponseEntity<Ride> completeRide(@RequestBody CompleteRideRequest request) {
        Ride ride = rideService.createRideForReward(request.getUserId(), request.getFare());
        
        rewardTreeService.insertNode(Long.parseLong(request.getUserId()));
        
        return ResponseEntity.ok(ride);
    }

    @Data
    public static class CompleteRideRequest {
        private String userId;
        private BigDecimal fare;
    }
}
