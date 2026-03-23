package com.urbanblack.fleetservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.urbanblack.fleetservice.enums.IssueCategory;
import com.urbanblack.fleetservice.enums.IssueSeverity;
import com.urbanblack.fleetservice.enums.IssueStatus;

@Entity
@Table(name = "issue_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private IssueCategory category; // vehicle, trip, other

    @Enumerated(EnumType.STRING)
    private IssueSeverity severity; // low, medium, high, critical
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String locationAddress;
    private Double latitude;
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String photos; // JSON array of base64 images

    private String vehicleId;
    private String tripId;
    private String driverId;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private IssueStatus status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    private String ticketNumber; // Auto-generated ticket number
}
