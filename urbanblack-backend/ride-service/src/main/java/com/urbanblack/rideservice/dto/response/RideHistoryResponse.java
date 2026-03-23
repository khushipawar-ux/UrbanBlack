package com.urbanblack.rideservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RideHistoryResponse {
    private List<RideHistoryItemResponse> rides;
}

