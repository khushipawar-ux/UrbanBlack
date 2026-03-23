package com.traffic.management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CenterPointRequest {
    private String pointName;
    private Double latitude;
    private Double longitude;
}
