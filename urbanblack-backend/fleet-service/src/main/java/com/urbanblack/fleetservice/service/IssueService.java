package com.urbanblack.fleetservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanblack.fleetservice.dto.IssueHistoryResponse;
import com.urbanblack.fleetservice.dto.IssueReportRequest;
import com.urbanblack.fleetservice.dto.IssueReportResponse;
import com.urbanblack.fleetservice.dto.PerformanceMetricsResponse;
import com.urbanblack.fleetservice.entity.IssueReport;
import com.urbanblack.fleetservice.entity.VehicleAssignment;
import com.urbanblack.fleetservice.exception.FleetServiceException;
import com.urbanblack.fleetservice.repository.IssueReportRepository;
import com.urbanblack.fleetservice.repository.VehicleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.urbanblack.fleetservice.enums.IssueStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueService {

    private final IssueReportRepository issueReportRepository;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final ObjectMapper objectMapper;

    // ===============================
    // 1️⃣ Submit Issue Report
    // ===============================
    @Transactional
    public IssueReportResponse submitIssueReport(String driverId, IssueReportRequest request) {
        log.info("Submitting issue report for driver: {}, category: {}", driverId, request.getCategory());
        validateDriverId(driverId);
        // Generate ticket number
        String ticketNumber = generateTicketNumber();

        // Convert photos list to JSON string
        String photosJson;
        try {
            photosJson = objectMapper.writeValueAsString(request.getPhotos());
        } catch (JsonProcessingException e) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ISSUE_PHOTOS",
                    "Unable to process issue photos");
        }

        IssueReport issueReport = IssueReport.builder()
                .category(request.getCategory())
                .severity(request.getSeverity())
                .title(request.getTitle())
                .description(request.getDescription())
                .locationAddress(request.getLocation() != null ? request.getLocation().getAddress() : null)
                .latitude(request.getLocation() != null ? request.getLocation().getLatitude() : null)
                .longitude(request.getLocation() != null ? request.getLocation().getLongitude() : null)
                .photos(photosJson)
                .vehicleId(request.getVehicleId())
                .tripId(request.getTripId())
                .driverId(driverId)
                .timestamp(parseTimestamp(request.getTimestamp()))
                .status(IssueStatus.OPEN)
                .ticketNumber(ticketNumber)
                .build();

        IssueReport savedReport = issueReportRepository.save(issueReport);
        log.info("Issue report saved with ID: {} and ticket: {}", savedReport.getId(), savedReport.getTicketNumber());

        return IssueReportResponse.builder()
                .id(savedReport.getId())
                // Normalize to lower-case for API
                .status(savedReport.getStatus() != null ? savedReport.getStatus().name().toLowerCase() : null)
                .ticketNumber(savedReport.getTicketNumber())
                .build();
    }

    // ===============================
    // 2️⃣ Get Performance Metrics
    // ===============================
    public PerformanceMetricsResponse getPerformanceMetrics(String driverId, String period) {
        validateDriverId(driverId);
        // This is a simplified implementation
        // In a real scenario, you would query trip data, calculate metrics, etc.
        // For now, returning default values as per API spec structure

        List<VehicleAssignment> assignments = vehicleAssignmentRepository
                .findAll()
                .stream()
                .filter(a -> driverId.equals(a.getDriverId()))
                .toList();

        int totalTrips = assignments.size();
        int completedTrips = (int) assignments.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
        int cancelledTrips = 0; // Would need to track cancellations separately

        int totalDistance = assignments.stream()
                .filter(a -> a.getStartKm() != null && a.getEndKm() != null)
                .mapToInt(a -> a.getEndKm() - a.getStartKm())
                .sum();

        // Default values for metrics that would come from other services
        return PerformanceMetricsResponse.builder()
                .period(period != null ? period : "month")
                .totalTrips(totalTrips)
                .completedTrips(completedTrips)
                .cancelledTrips(cancelledTrips)
                .totalDistance(totalDistance)
                .totalDuration(0L) // Would need trip duration data
                .averageRating(0.0) // Would come from trip/rating service
                .totalEarnings(0.0) // Would come from trip/payment service
                .fuelEfficiency(0.0) // Would need to calculate from fuel entries
                .punctualityScore(0) // Would need trip timing data
                .customerSatisfaction(0.0) // Would come from rating service
                .safetyScore(0) // Would need safety metrics
                .build();
    }

    // ===============================
    // 3️⃣ Get Issue History
    // ===============================
    public List<IssueHistoryResponse> getIssueHistory(String driverId) {
        log.info("Fetching issue history for driver: {}", driverId);
        validateDriverId(driverId);

        List<IssueReport> reports = issueReportRepository.findByDriverIdOrderByTimestampDesc(driverId);

        return reports.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    private IssueHistoryResponse mapToHistoryResponse(IssueReport report) {
        return IssueHistoryResponse.builder()
                .id(report.getId())
                .ticketNumber(report.getTicketNumber())
                .title(report.getTitle())
                .description(report.getDescription())
                .category(report.getCategory() != null ? report.getCategory().name().toLowerCase() : "other")
                .severity(report.getSeverity() != null ? report.getSeverity().name().toLowerCase() : "medium")
                .status(report.getStatus() != null ? report.getStatus().name().toLowerCase() : "open")
                .createdAt(report.getTimestamp())
                .hasPhotos(report.getPhotos() != null && !report.getPhotos().equals("[]"))
                .build();
    }

    private String generateTicketNumber() {
        // Generate ticket number in format: ISSUE-YYYY-NNNNNN
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = issueReportRepository.count();
        String sequence = String.format("%06d", count + 1);
        return "ISSUE-" + year + "-" + sequence;
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TIMESTAMP",
                    "Invalid timestamp format; expected ISO-8601 string");
        }
    }

    private void validateDriverId(String driverId) {
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new FleetServiceException(
                    HttpStatus.BAD_REQUEST,
                    "DRIVER_ID_REQUIRED",
                    "X-Driver-Id header is required");
        }
    }
}