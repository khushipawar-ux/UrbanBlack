package com.urbanblack.rideservice.service;

import com.urbanblack.rideservice.entity.FareConfig;
import com.urbanblack.rideservice.repository.FareConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FareService {

    private final FareConfigRepository fareConfigRepository;

    public FareConfig getCurrentConfigOrDefault() {
        return fareConfigRepository.findTopByOrderByIdDesc()
                .orElseGet(() -> FareConfig.builder()
                        .perKmRate(BigDecimal.valueOf(15))
                        .minFare(BigDecimal.valueOf(50))
                        .build());
    }

    public BigDecimal calculateFare(double rideKm) {
        FareConfig config = getCurrentConfigOrDefault();
        BigDecimal fare = config.getPerKmRate().multiply(BigDecimal.valueOf(rideKm));
        if (fare.compareTo(config.getMinFare()) < 0) {
            fare = config.getMinFare();
        }
        return fare;
    }
}
