package com.urban.cabregisterationservice.dto;

import com.urbanblack.common.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CabApplicationResponseDTO {
    private Long cabApplicationId;
    private String username;
    private ApplicationStatus status;
    private LocalDateTime createdDate;
}
//
