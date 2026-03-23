package com.urbanblack.fleetservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueHistoryResponse {
    private String id;
    private String ticketNumber;
    private String title;
    private String description;
    private String category;
    private String severity;
    private String status;
    private LocalDateTime createdAt;
    private boolean hasPhotos;
}

