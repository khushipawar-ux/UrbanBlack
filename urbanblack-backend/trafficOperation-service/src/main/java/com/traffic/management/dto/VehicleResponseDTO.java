package com.traffic.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponseDTO {
    private Long cabApplicationId;
    private String username;
    private String numberPlate;
    private String carName;
    private String status;
    private RcDetailsDTO rcNumber;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RcDetailsDTO {
        private String rcNumber;
        private String ownerName;
        private String vehicleChasiNumber;
        private String vehicleEngineNumber;
        private String rcStatus;
    }
}
