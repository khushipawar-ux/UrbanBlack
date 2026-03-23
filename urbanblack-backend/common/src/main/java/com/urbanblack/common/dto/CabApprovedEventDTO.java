package com.urbanblack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CabApprovedEventDTO {
    private Long cabId;
    private String carNumber;
    private Long ownerId;
}
