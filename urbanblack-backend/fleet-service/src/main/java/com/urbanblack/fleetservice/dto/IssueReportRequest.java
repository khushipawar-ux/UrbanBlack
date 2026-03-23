package com.urbanblack.fleetservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import com.urbanblack.fleetservice.enums.IssueCategory;
import com.urbanblack.fleetservice.enums.IssueSeverity;

@Data
public class IssueReportRequest {
    @NotNull(message = "category is required")
    private IssueCategory category;
    @NotNull(message = "severity is required")
    private IssueSeverity severity;
    @NotBlank(message = "title is required")
    private String title;
    @NotBlank(message = "description is required")
    private String description;

    @Valid
    private Location location;
    private List<String> photos; // Base64 encoded images
    private String vehicleId;
    private String tripId;
    private String timestamp;

    @Data
    public static class Location {
        @NotNull(message = "location.latitude is required")
        private Double latitude;
        @NotNull(message = "location.longitude is required")
        private Double longitude;
        @NotBlank(message = "location.address is required")
        private String address;
    }
}
