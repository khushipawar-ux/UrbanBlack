package com.urbanblack.notification.dto;

import com.urbanblack.notification.entity.enums.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after a notification is queued/sent")
public class NotificationResponse {

    @Schema(description = "ID of the persisted notification log", example = "101")
    private Long id;

    @Schema(description = "Delivery status", example = "SENT")
    private NotificationStatus status;

    @Schema(description = "Human-readable status message", example = "Email sent successfully to rajesh.kumar@urbanblack.in")
    private String message;

    @Schema(description = "Timestamp of the attempt")
    private LocalDateTime timestamp;
}
