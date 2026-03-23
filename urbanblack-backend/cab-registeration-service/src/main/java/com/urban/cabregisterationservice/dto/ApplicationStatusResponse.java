package com.urban.cabregisterationservice.dto;

import com.urbanblack.common.enums.CabModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusResponse {
    private Long applicationId;
    private CabModel cabModel;
    private String currentStage;
    private String overallStatus;
}

//
