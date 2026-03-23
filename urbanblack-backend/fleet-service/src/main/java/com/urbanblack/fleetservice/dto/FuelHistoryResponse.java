package com.urbanblack.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelHistoryResponse {
    private String id;
    private LocalDateTime date;
    private String fuelType;
    private Double quantity;
    private Double amount;
    private String stationName;
    private String status;
}
