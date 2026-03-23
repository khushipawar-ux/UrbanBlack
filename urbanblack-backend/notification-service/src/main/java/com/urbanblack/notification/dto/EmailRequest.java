package com.urbanblack.notification.dto;

import com.urbanblack.notification.entity.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EmailRequest DTO
 *
 * Used by other microservices to send notifications via REST API.
 * Also consumed from Kafka events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send an email notification")
public class EmailRequest {

    @Email
    @NotBlank
    @Schema(description = "Recipient email address", example = "rajesh.kumar@urbanblack.in")
    private String to;

    @Schema(description = "Recipient display name", example = "Rajesh Kumar")
    private String recipientName;

    @NotBlank
    @Schema(description = "Email subject", example = "Your Login Credentials")
    private String subject;

    @Schema(description = "Plain text body (used if htmlBody is null)")
    private String body;

    @Schema(description = "HTML body (takes priority over plain text body)")
    private String htmlBody;

    @NotNull
    @Schema(description = "Type of notification", example = "EMPLOYEE_CREDENTIALS")
    private NotificationType type;

    @Schema(description = "Source service name for audit log", example = "employee-details-service")
    private String sourceService;

    @Schema(description = "Reference ID in source service (e.g. employee ID)", example = "42")
    private String referenceId;
}
