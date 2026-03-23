package com.urbanblack.rideservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardResult {
    private List<UplinePayout> uplines;
    private Double adminAmount;
    private Double total;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UplinePayout {
        private String userId;
        private Double amount;
    }
}
