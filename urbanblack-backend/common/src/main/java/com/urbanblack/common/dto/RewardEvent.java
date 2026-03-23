package com.urbanblack.common.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardEvent {
    private Long triggeringNodeId;
    private List<UplineRecord> uplines;
    private Double totalDeduction;
    private Double rewardPerLevel;
}
