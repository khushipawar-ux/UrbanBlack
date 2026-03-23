package com.traffic.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepotResponse {
    private Long id;
    private String depotCode;
    private String depotName;
    private String city;
    private String fullAddress;
    private Boolean isActive;
}
