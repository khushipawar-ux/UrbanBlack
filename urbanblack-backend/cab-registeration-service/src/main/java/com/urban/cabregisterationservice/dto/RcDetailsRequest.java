package com.urban.cabregisterationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RcDetailsRequest {
    private String rcNumber;
    private String chassisNumber;
    private String engineNumber;
}
//