package EmployeeDetails_Service.EmployeeDetails_Service.Service;

import EmployeeDetails_Service.EmployeeDetails_Service.Entity.Employee;
import EmployeeDetails_Service.EmployeeDetails_Service.Entity.EmployeePackage;
import EmployeeDetails_Service.EmployeeDetails_Service.REPO.EmployeePackageRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Credential Mail Service
 *
 * Sends a professional HTML welcome email to newly onboarded employees
 * containing their auto-generated username and temporary password.
 */
@Service
@Slf4j
public class CredentialMailService {

    private final JavaMailSender mailSender;
    private final EmployeePackageRepository employeePackageRepository;

    @Value("${spring.mail.username:not-configured}")
    private String mailUsername;

    @Autowired
    public CredentialMailService(JavaMailSender mailSender, EmployeePackageRepository employeePackageRepository) {
        this.mailSender = mailSender;
        this.employeePackageRepository = employeePackageRepository;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("📧 CredentialMailService initialized with mailUsername: {}", mailUsername);
    }

    /**
     * Sends login credentials to the employee asynchronously.
     * Called automatically when a new employee is created.
     *
     * Skips silently if mail is not configured (local dev without MAIL_USERNAME set).
     */
    @Async
    public void sendCredentials(Employee employee) {
        log.info("📧 Attempting to send credentials email to: {} using sender: {}", employee.getEmail(), mailUsername);
        // Skip if mail not configured (placeholder value)
        if ("not-configured".equals(mailUsername) || mailUsername.startsWith("your-gmail")) {
            log.warn("📧 Mail not configured — skipping credential email for '{}'." +
                     " Set MAIL_USERNAME and MAIL_PASSWORD env vars to enable emails.", employee.getEmail());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(employee.getEmail());
            helper.setSubject("🎉 Welcome to Urban Black – Your Login Credentials");
            helper.setFrom(mailUsername);
            helper.setText(buildHtmlEmail(employee), true);

            mailSender.send(message);
            log.info("✅ Credentials email sent to {}", employee.getEmail());

        } catch (MessagingException e) {
            log.error("❌ Failed to send credentials email to {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    /**
     * Builds a professional HTML email body with employee credentials.
     */
    private String buildHtmlEmail(Employee employee) {
        String roleDisplay = formatRole(employee.getRole() != null ? employee.getRole().name() : "EMPLOYEE");

        // Fetch LATEST Employee Package data dynamically
        EmployeePackage empPackage = employeePackageRepository.findFirstByOrderByIdDesc()
                .orElse(EmployeePackage.builder()
                        .designation("Driver Trainee")
                        .durationMonths(6)
                        .inHandSalary(24000.0)
                        .monthlyOff(4)
                        .build());

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    body { margin: 0; padding: 0; background-color: #f4f4f5; font-family: 'Segoe UI', Arial, sans-serif; }
                    .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
                    .header { background: #000000; padding: 36px 40px; text-align: center; }
                    .header h1 { color: #ffffff; margin: 0; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
                    .header p { color: #a0a0a0; margin: 6px 0 0; font-size: 14px; }
                    .body { padding: 36px 40px; }
                    .greeting { font-size: 16px; color: #1a1a1a; margin-bottom: 20px; line-height: 1.6; }
                    .role-badge { display: inline-block; background: #f0f9ff; color: #0369a1; border: 1px solid #bae6fd; border-radius: 20px; padding: 4px 14px; font-size: 13px; font-weight: 600; margin-bottom: 28px; }
                    .package-box { background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .package-box h4 { margin: 0 0 12px; color: #334155; font-size: 14px; border-bottom: 1px solid #e2e8f0; padding-bottom: 8px; }
                    .package-row { margin-bottom: 8px; font-size: 14px; color: #475569; }
                    .package-label { font-weight: 600; color: #1e293b; width: 140px; display: inline-block; }
                    .credentials-box { background: #fafafa; border: 2px solid #e5e7eb; border-radius: 10px; padding: 24px 28px; margin: 20px 0; }
                    .credentials-box h3 { margin: 0 0 16px; font-size: 15px; color: #6b7280; text-transform: uppercase; letter-spacing: 1px; }
                    .credential-row { display: flex; align-items: center; margin-bottom: 14px; }
                    .credential-label { width: 130px; font-size: 13px; color: #9ca3af; font-weight: 600; text-transform: uppercase; }
                    .credential-value { font-size: 16px; color: #111827; font-weight: 700; font-family: 'Courier New', monospace; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; padding: 6px 14px; letter-spacing: 1px; }
                    .notice { background: #fffbeb; border-left: 4px solid #f59e0b; border-radius: 4px; padding: 14px 18px; margin: 24px 0; font-size: 14px; color: #92400e; line-height: 1.6; }
                    .steps { margin: 24px 0; }
                    .steps h3 { font-size: 15px; color: #374151; margin-bottom: 14px; }
                    .step { display: flex; align-items: flex-start; margin-bottom: 12px; font-size: 14px; color: #4b5563; line-height: 1.5; }
                    .step-num { background: #000; color: #fff; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; margin-right: 12px; margin-top: 1px; }
                    .btn { display: block; width: fit-content; background: #000000; color: #ffffff !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 700; font-size: 15px; margin: 28px auto; text-align: center; }
                    .footer { background: #f9fafb; border-top: 1px solid #e5e7eb; padding: 24px 40px; text-align: center; font-size: 12px; color: #9ca3af; line-height: 1.6; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <h1>Urban Black</h1>
                      <p>Employee Onboarding</p>
                    </div>
                    <div class="body">
                      <div class="greeting">
                        Hello <strong>%s</strong>, 👋<br><br>
                        Welcome to the <strong>Urban Black</strong> team! Your employee account has been created successfully.
                        Below are your package details and login credentials.
                      </div>

                      <span class="role-badge">🏷️ %s</span>

                      <div class="package-box">
                        <h4>📅 Training Period (First %d Months)</h4>
                        <div class="package-row"><span class="package-label">Designation:</span> %s</div>
                        <div class="package-row"><span class="package-label">Duration:</span> %d Months</div>
                        <div class="package-row"><span class="package-label">In-Hand Salary:</span> ₹%.2f per month</div>
                        <div class="package-row"><span class="package-label">Monthly Off:</span> %d days per month</div>
                        <div class="package-row"><span class="package-label">Medical Insurance:</span> %s</div>
                      </div>

                      <div class="credentials-box">
                        <h3>🔐 Login Credentials</h3>
                        <div class="credential-row">
                          <span class="credential-label">Username</span>
                          <span class="credential-value">%s</span>
                        </div>
                        <div class="credential-row">
                          <span class="credential-label">Password</span>
                          <span class="credential-value">%s</span>
                        </div>
                      </div>

                      <div class="notice">
                        ⚠️ <strong>Important:</strong> This is a temporary password. Please log in and change it immediately for security.
                      </div>

                      <div class="steps">
                        <h3>📋 Getting Started</h3>
                        <div class="step"><div class="step-num">1</div>Visit the Urban Black Admin Portal.</div>
                        <div class="step"><div class="step-num">2</div>Enter your username and temporary password.</div>
                        <div class="step"><div class="step-num">3</div>Change your password on first login.</div>
                        <div class="step"><div class="step-num">4</div>Complete your profile verification.</div>
                      </div>

                      <a href="http://localhost:5173" class="btn">Login to Portal →</a>
                    </div>
                    <div class="footer">
                      This email was sent automatically by Urban Black HR System.<br>
                      If you have questions, contact <a href="mailto:hr@urbanblack.com">hr@urbanblack.com</a><br><br>
                      &copy; 2025 Urban Black. All rights reserved.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                        employee.getFullName(),
                        roleDisplay,
                        empPackage.getDurationMonths(),
                        empPackage.getDesignation(),
                        empPackage.getDurationMonths(),
                        empPackage.getInHandSalary(),
                        empPackage.getMonthlyOff(),
                        employee.getMedicalInsuranceNumber() != null ? employee.getMedicalInsuranceNumber() : "N/A",
                        employee.getUsername(),
                        employee.getTempPassword()
                );
    }

    /**
     * Formats a role enum name to a human-readable display string.
     * e.g. "DEPOT_MANAGER" → "Depot Manager"
     */
    private String formatRole(String role) {
        return java.util.Arrays.stream(role.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
