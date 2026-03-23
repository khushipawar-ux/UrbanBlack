package com.urban.cabregisterationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPackageRequest {
    private BigDecimal monthlyPackage;
    private Double monthlyKm;
    private Double dailyHours;
    private Integer monthlyDaysCover;
    private Double perDayKm;
    private BigDecimal vendorPerDayPackage;
}
