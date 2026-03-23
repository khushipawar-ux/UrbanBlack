package com.traffic.management.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAssignmentRequest {
    private LocalDate assignmentDate;
    private Long depotId;
    private List<Long> driverIds;
    private List<Long> cabIds;
    private Long shiftId;
    private Long centerPointId;
    private String createdBy;
}
