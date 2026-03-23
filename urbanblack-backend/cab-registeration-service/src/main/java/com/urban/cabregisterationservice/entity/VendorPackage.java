package com.urban.cabregisterationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "vendor_package")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VendorPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal monthlyPackage;

    private Double monthlyKm;

    private Double dailyHours;

    private Integer monthlyDaysCover;

    private Double perDayKm;

    private BigDecimal vendorPerDayPackage;
}
