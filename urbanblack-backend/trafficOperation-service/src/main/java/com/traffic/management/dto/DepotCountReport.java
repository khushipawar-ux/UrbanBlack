package com.traffic.management.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class DepotCountReport {
    private String depotName;
    private long count;
}
