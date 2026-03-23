package com.urbanblack.fleetservice.controller;

import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.fleetservice.dto.IssueHistoryResponse;
import com.urbanblack.fleetservice.dto.IssueReportRequest;
import com.urbanblack.fleetservice.dto.IssueReportResponse;
import com.urbanblack.fleetservice.dto.PerformanceMetricsResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.urbanblack.fleetservice.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Validated
public class IssueController {

    private final IssueService issueService;

    // ===============================
    // 1️⃣ Submit Issue Report
    // ===============================
    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<IssueReportResponse>> submitIssueReport(
            @RequestHeader("X-Driver-Id") @NotBlank(message = "X-Driver-Id header is required") String driverId,
            @Valid @RequestBody IssueReportRequest request) {

        IssueReportResponse response = issueService.submitIssueReport(driverId, request);

        return ResponseEntity.ok(ApiResponse.<IssueReportResponse>builder()
                .success(true)
                .data(response)
                .message("Issue reported successfully")
                .build());
    }

    // ===============================
    // 2️⃣ Get Performance Metrics
    // ===============================
    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<PerformanceMetricsResponse>> getPerformanceMetrics(
            @RequestHeader("X-Driver-Id") @NotBlank(message = "X-Driver-Id header is required") String driverId,
            @RequestParam(defaultValue = "month") String period) {

        PerformanceMetricsResponse metrics = issueService.getPerformanceMetrics(driverId, period);

        return ResponseEntity.ok(ApiResponse.<PerformanceMetricsResponse>builder()
                .success(true)
                .data(metrics)
                .build());
    }

    // ===============================
    // 3️⃣ Get Issue History
    // ===============================
    @GetMapping("/issue/history")
    public ResponseEntity<ApiResponse<List<IssueHistoryResponse>>> getIssueHistory(
            @RequestHeader("X-Driver-Id") @NotBlank(message = "X-Driver-Id header is required") String driverId) {

        List<IssueHistoryResponse> history = issueService.getIssueHistory(driverId);

        return ResponseEntity.ok(ApiResponse.<List<IssueHistoryResponse>>builder()
                .success(true)
                .data(history)
                .message("Issue history fetched successfully")
                .build());
    }
}