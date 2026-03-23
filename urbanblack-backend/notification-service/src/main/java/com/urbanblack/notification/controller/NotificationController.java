package com.urbanblack.notification.controller;

import com.urbanblack.notification.dto.EmailRequest;
import com.urbanblack.notification.dto.NotificationResponse;
import com.urbanblack.notification.entity.NotificationLog;
import com.urbanblack.notification.entity.enums.NotificationStatus;
import com.urbanblack.notification.service.EmailNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NotificationController
 *
 * REST API for:
 *  1. Sending notifications (sync or async) from any microservice
 *  2. Querying notification log history for admin dashboard
 *
 * Base path: /notifications
 * Swagger:   http://localhost:8091/swagger-ui/index.html
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Send emails and query notification history")
public class NotificationController {

    private final EmailNotificationService emailService;

    // ── Send ──────────────────────────────────────────────────────────────────

    @PostMapping("/email/send")
    @Operation(
        summary = "Send Email Notification (Sync)",
        description = "Sends an email immediately and returns the result. " +
                      "Other microservices can call this endpoint to trigger notifications."
    )
    @ApiResponse(responseCode = "200", description = "Email sent or failed with details",
        content = @Content(schema = @Schema(implementation = NotificationResponse.class),
            examples = @ExampleObject(value = """
                {
                  "id": 1,
                  "status": "SENT",
                  "message": "Email sent successfully to rajesh.kumar@urbanblack.in",
                  "timestamp": "2025-02-21T16:47:10"
                }
                """)))
    public ResponseEntity<NotificationResponse> sendEmail(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Email notification request",
                content = @Content(schema = @Schema(implementation = EmailRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "to": "rajesh.kumar@urbanblack.in",
                          "recipientName": "Rajesh Kumar",
                          "subject": "Test Notification",
                          "body": "Hello from Urban Black Notification Service!",
                          "type": "CUSTOM",
                          "sourceService": "admin-panel"
                        }
                        """)))
            @Valid @RequestBody EmailRequest request) {

        NotificationResponse response = emailService.sendSync(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/send-async")
    @Operation(
        summary = "Send Email Notification (Async)",
        description = "Queues the email and returns immediately. " +
                      "Use this for non-blocking notification sending."
    )
    @ApiResponse(responseCode = "202", description = "Email queued for sending")
    public ResponseEntity<String> sendEmailAsync(
            @Valid @RequestBody EmailRequest request) {
        emailService.sendAsync(request);
        return ResponseEntity.accepted().body("Notification queued for: " + request.getTo());
    }

    @PostMapping("/employee-credentials")
    @Operation(
        summary = "Send Employee Credentials Email",
        description = "Dedicated endpoint for employee onboarding — sends a branded credential email."
    )
    @ApiResponse(responseCode = "200", description = "Credentials email sent")
    public ResponseEntity<NotificationResponse> sendEmployeeCredentials(
            @Valid @RequestBody com.urbanblack.notification.dto.CredentialEmailRequest request) {

        NotificationResponse response = emailService.sendEmployeeCredentials(
                request.getEmail(), 
                request.getFullName(), 
                request.getRole(), 
                request.getUsername(), 
                request.getTempPassword(), 
                request.getEmployeeId(),
                request.getDesignation(),
                request.getDurationMonths(),
                request.getInHandSalary(),
                request.getMonthlyOff(),
                request.getMedicalInsuranceNumber());
        return ResponseEntity.ok(response);
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    @GetMapping("/logs")
    @Operation(summary = "Get All Notification Logs", description = "Returns full notification history for admin dashboard")
    public ResponseEntity<List<NotificationLog>> getAllLogs() {
        return ResponseEntity.ok(emailService.getAllLogs());
    }

    @GetMapping("/logs/email/{email}")
    @Operation(summary = "Get Logs by Recipient Email", description = "Filter notification history by recipient email address")
    public ResponseEntity<List<NotificationLog>> getLogsByEmail(
            @PathVariable String email) {
        return ResponseEntity.ok(emailService.getLogsByEmail(email));
    }

    @GetMapping("/logs/status/{status}")
    @Operation(summary = "Get Logs by Status", description = "Filter by PENDING, SENT, or FAILED")
    public ResponseEntity<List<NotificationLog>> getLogsByStatus(
            @PathVariable NotificationStatus status) {
        return ResponseEntity.ok(emailService.getLogsByStatus(status));
    }

    @GetMapping("/logs/service/{service}")
    @Operation(summary = "Get Logs by Source Service", description = "Filter logs by the microservice that triggered the notification")
    public ResponseEntity<List<NotificationLog>> getLogsByService(
            @PathVariable String service) {
        return ResponseEntity.ok(emailService.getLogsBySourceService(service));
    }
}
