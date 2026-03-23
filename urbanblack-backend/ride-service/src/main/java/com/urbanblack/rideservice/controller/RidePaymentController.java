package com.urbanblack.rideservice.controller;

import com.urbanblack.rideservice.entity.Ride;
import com.urbanblack.rideservice.repository.RideRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RidePaymentController {

    private final RideRepository rideRepository;

    @PatchMapping("/{rideId}/payment")
    public ResponseEntity<Ride> updatePayment(
            @PathVariable String rideId,
            @RequestBody UpdatePaymentRequest request) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        ride.setWalletUsed(request.getWalletUsed());
        ride.setOnlinePaid(request.getOnlinePaid());
        ride.setPaymentStatus(request.getPaymentStatus());
        ride.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(rideRepository.save(ride));
    }

    @Data
    public static class UpdatePaymentRequest {
        private BigDecimal walletUsed;
        private BigDecimal onlinePaid;
        private Ride.PaymentStatus paymentStatus;
    }
}

