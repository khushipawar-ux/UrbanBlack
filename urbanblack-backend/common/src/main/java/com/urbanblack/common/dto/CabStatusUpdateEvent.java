package com.urbanblack.common.dto;

import com.urbanblack.common.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CabStatusUpdateEvent {
    private Long cabApplicationId;
    private ApplicationStatus status;
}
