package com.urbanblack.common.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UplineRecord {
    private Long nodeId;
    private Long userId;
    private Integer level;
    private Boolean active;
}
