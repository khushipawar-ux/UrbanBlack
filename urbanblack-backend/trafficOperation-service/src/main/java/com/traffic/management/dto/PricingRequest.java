package com.traffic.management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricingRequest {

    private Double distance;
    private String vehicleType; // AC or NON_AC
    private Integer parkingCount;
}