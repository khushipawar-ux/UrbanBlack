package com.traffic.management.dto;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class PricingResponse {

    private Double baseFare;
    private Double parkingCharge;
    private Double totalFare;
    private Integer stageId;
}
