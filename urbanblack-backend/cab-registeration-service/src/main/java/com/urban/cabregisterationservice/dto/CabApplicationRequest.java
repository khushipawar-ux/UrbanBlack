package com.urban.cabregisterationservice.dto;

import com.urbanblack.common.enums.CabCategory;
import com.urbanblack.common.enums.CabModel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request DTO for new Cab Application with full vehicle details")
public class CabApplicationRequest {

    @Schema(description = "Internal Car ID (Optional)", example = "101")
    private Long carId;

    @Schema(description = "Model family of the car", example = "WAGONR")
    private CabModel cabModel;

    @Schema(description = "Specific name/edition of the car", example = "Toyota Innova")
    private String carName;

    @Schema(description = "AC or NON-AC", example = "AC")
    private String acType;

    @Schema(description = "Registration Number (Car NO)", example = "MH12AB1234")
    private String carNumber;

    @Schema(description = "Engine Chassis Number", example = "CHAS1234567890")
    private String chassisNumber;

    @Schema(description = "Engine Number", example = "ENG6543210")
    private String engineNumber;

    @Schema(description = "Registration Certificate ID", example = "RC-7890")
    private String rcId;

    @Schema(description = "Year of manufacture", example = "2024")
    private Integer year;

    @Schema(description = "Current application status (Optional)", example = "PENDING_ADMIN_APPROVAL")
    private String status;

    @Schema(description = "Current application stage (Optional)", example = "ROUND_0")
    private String stage;

    @Schema(description = "RTO Passing/Registration Date", example = "2024-05-20")
    private String passingDate;

    @Schema(description = "Calculated Package Amount", example = "45000.00")
    private BigDecimal packageAmount;

    @Schema(description = "Fuel Type (Petrol, Diesel, EV, etc.)", example = "Petrol/Hybrid")
    private String fuelType;

    @Schema(description = "Current Kilometers on Odometer", example = "15000")
    private Long kms;

    @Schema(description = "RC Number for Verification", example = "MH12AB1234")
    private String rcNumberVerification;

    @Schema(description = "Cab category (ECONOMY, PREMIUM)", example = "ECONOMY")
    private CabCategory category;
}