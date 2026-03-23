package com.urbanblack.notification.service;

import com.urbanblack.notification.dto.EmailRequest;
import com.urbanblack.notification.dto.NotificationResponse;
import com.urbanblack.notification.entity.NotificationLog;
import com.urbanblack.notification.entity.enums.NotificationStatus;
import com.urbanblack.notification.repository.NotificationLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EmailNotificationService
 *
 * Core service responsible for:
 * - Sending HTML/plain-text emails via JavaMailSender
 * - Persisting every attempt to NotificationLog
 * - Exposing log query methods for the admin REST API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository logRepository;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String mailFrom;

    // ── Send (Async) ──────────────────────────────────────────────────────────

    /**
     * Sends an email asynchronously and persists the result.
     * Called from REST API or Kafka consumer.
     */
    @Async
    public void sendAsync(EmailRequest request) {
        NotificationLog logEntry = createPendingLog(request);
        logRepository.save(logEntry);
        doSend(request, logEntry);
    }

    /**
     * Sends an email synchronously and returns the result.
     * Called from REST API when the caller needs immediate feedback.
     */
    public NotificationResponse sendSync(EmailRequest request) {
        NotificationLog logEntry = createPendingLog(request);
        logRepository.save(logEntry);
        doSend(request, logEntry);

        return NotificationResponse.builder()
                .id(logEntry.getId())
                .status(logEntry.getStatus())
                .message(logEntry.getStatus() == NotificationStatus.SENT
                        ? "Email sent successfully to " + request.getTo()
                        : "Failed to send email: " + logEntry.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ── Log Queries ───────────────────────────────────────────────────────────

    public List<NotificationLog> getAllLogs() {
        return logRepository.findAll();
    }

    public List<NotificationLog> getLogsByEmail(String email) {
        return logRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
    }

    public List<NotificationLog> getLogsByStatus(NotificationStatus status) {
        return logRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<NotificationLog> getLogsBySourceService(String service) {
        return logRepository.findBySourceServiceOrderByCreatedAtDesc(service);
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private void doSend(EmailRequest request, NotificationLog logEntry) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setFrom(mailFrom);

            // Prefer HTML body; fall back to plain text
            if (request.getHtmlBody() != null && !request.getHtmlBody().isBlank()) {
                helper.setText(request.getHtmlBody(), true);
            } else {
                helper.setText(request.getBody() != null ? request.getBody() : "", false);
            }

            mailSender.send(message);

            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setUpdatedAt(LocalDateTime.now());
            log.info("✅ Email sent → {}", request.getTo());

        } catch (Exception e) {
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            logEntry.setRetryCount(logEntry.getRetryCount() + 1);
            logEntry.setUpdatedAt(LocalDateTime.now());
            log.error("❌ Email failed → {} : {}", request.getTo(), e.getMessage(), e);
        } finally {
            logRepository.save(logEntry);
        }
    }

    private NotificationLog createPendingLog(EmailRequest request) {
        return NotificationLog.builder()
                .recipientEmail(request.getTo())
                .recipientName(request.getRecipientName())
                .subject(request.getSubject())
                .type(request.getType())
                .status(NotificationStatus.PENDING)
                .sourceService(request.getSourceService())
                .referenceId(request.getReferenceId())
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    // ── Template Builders ─────────────────────────────────────────────────────

    /**
     * Builds and sends an employee credential email.
     * Other services call this via Kafka or REST.
     */
    public NotificationResponse sendEmployeeCredentials(
            String toEmail, String fullName, String role,
            String username, String tempPassword, String employeeId,
            String designation, Integer durationMonths, Double inHandSalary, Integer monthlyOff, String medicalInsuranceNumber) {

        String html = buildCredentialHtml(fullName, role, username, tempPassword, 
                                        designation, durationMonths, inHandSalary, monthlyOff, medicalInsuranceNumber);

        EmailRequest request = EmailRequest.builder()
                .to(toEmail)
                .recipientName(fullName)
                .subject("🎉 Welcome to Urban Black – Your Login Credentials")
                .htmlBody(html)
                .type(com.urbanblack.notification.entity.enums.NotificationType.EMPLOYEE_CREDENTIALS)
                .sourceService("employee-details-service")
                .referenceId(employeeId)
                .build();

        return sendSync(request);
    }

    private String buildCredentialHtml(String name, String role, String username, String password,
                                      String designation, Integer durationMonths, Double inHandSalary, Integer monthlyOff, String medicalInsuranceNumber) {
        String roleDisplay = java.util.Arrays.stream(role.split("_"))
                .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
                .collect(java.util.stream.Collectors.joining(" "));

        // Safeguard for null package values
        String actualDesignation = designation != null ? designation : "Employee";
        int actualDuration = durationMonths != null ? durationMonths : 6;
        double actualSalary = inHandSalary != null ? inHandSalary : 0.0;
        int actualOff = monthlyOff != null ? monthlyOff : 4;
        String actualInsurance = medicalInsuranceNumber != null ? medicalInsuranceNumber : "N/A";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { margin:0; padding:0; background:#f4f4f5; font-family:'Segoe UI',Arial,sans-serif; }
                    .container { max-width:600px; margin:40px auto; background:#fff; border-radius:12px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,.08); }
                    .header { background:#000; padding:36px 40px; text-align:center; }
                    .header h1 { color:#fff; margin:0; font-size:28px; font-weight:700; }
                    .header p  { color:#a0a0a0; margin:6px 0 0; font-size:14px; }
                    .body { padding:36px 40px; }
                    .greeting { font-size:16px; color:#1a1a1a; line-height:1.6; margin-bottom:20px; }
                    .badge { display:inline-block; background:#f0f9ff; color:#0369a1; border:1px solid #bae6fd; border-radius:20px; padding:4px 14px; font-size:13px; font-weight:600; margin-bottom:28px; }
                    .pbox { background:#f8fafc; border:1px dashed #cbd5e1; border-radius:8px; padding:20px; margin:20px 0; }
                    .pbox h4 { margin:0 0 12px; color:#334155; font-size:14px; border-bottom:1px solid #e2e8f0; padding-bottom:8px; }
                    .prow { margin-bottom:8px; font-size:14px; color:#475569; }
                    .plabel { font-weight:600; color:#1e293b; width:140px; display:inline-block; }
                    .cbox { background:#fafafa; border:2px solid #e5e7eb; border-radius:10px; padding:24px 28px; margin:20px 0; }
                    .cbox h3 { margin:0 0 16px; font-size:13px; color:#6b7280; text-transform:uppercase; letter-spacing:1px; }
                    .crow { display:flex; align-items:center; margin-bottom:14px; }
                    .clabel { width:130px; font-size:13px; color:#9ca3af; font-weight:600; text-transform:uppercase; }
                    .cval { font-size:16px; color:#111827; font-weight:700; font-family:'Courier New',monospace; background:#fff; border:1px solid #e5e7eb; border-radius:6px; padding:6px 14px; }
                    .notice { background:#fffbeb; border-left:4px solid #f59e0b; padding:14px 18px; margin:24px 0; font-size:14px; color:#92400e; line-height:1.6; border-radius:4px; }
                    .btn { display:block; width:fit-content; background:#000; color:#fff!important; text-decoration:none; padding:14px 32px; border-radius:8px; font-weight:700; font-size:15px; margin:28px auto; text-align:center; }
                    .footer { background:#f9fafb; border-top:1px solid #e5e7eb; padding:24px 40px; text-align:center; font-size:12px; color:#9ca3af; line-height:1.6; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header"><h1>Urban Black</h1><p>Employee Onboarding</p></div>
                    <div class="body">
                      <div class="greeting">Hello <strong>%s</strong> 👋<br><br>Welcome to <strong>Urban Black</strong>! Your account is ready. Here are your package details and login credentials.</div>
                      <span class="badge">🏷️ %s</span>

                      <div class="pbox">
                        <h4>📅 Training Period (First %d Months)</h4>
                        <div class="prow"><span class="plabel">Designation:</span> %s</div>
                        <div class="prow"><span class="plabel">Duration:</span> %d Months</div>
                        <div class="prow"><span class="plabel">In-Hand Salary:</span> ₹%.2f per month</div>
                        <div class="prow"><span class="plabel">Monthly Off:</span> %d days per month</div>
                        <div class="prow"><span class="plabel">Medical Insurance:</span> %s</div>
                      </div>

                      <div class="cbox">
                        <h3>🔐 Login Credentials</h3>
                        <div class="crow"><span class="clabel">Username</span><span class="cval">%s</span></div>
                        <div class="crow"><span class="clabel">Password</span><span class="cval">%s</span></div>
                      </div>
                      <div class="notice">⚠️ <strong>Important:</strong> Change your password immediately after first login.</div>
                      <a href="http://localhost:5173" class="btn">Login to Portal →</a>
                    </div>
                    <div class="footer">&copy; 2025 Urban Black · <a href="mailto:hr@urbanblack.com">hr@urbanblack.com</a></div>
                  </div>
                </body>
                </html>
                """.formatted(name, roleDisplay, actualDuration, actualDesignation, actualDuration, actualSalary, actualOff, actualInsurance, username, password);
    }
}
