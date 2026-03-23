package EmployeeDetails_Service.EmployeeDetails_Service.Entity;

import EmployeeDetails_Service.EmployeeDetails_Service.Entity.enums.EmployeeRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.urbanblack.common.enums.AccountStatus;
import com.urbanblack.common.enums.VerificationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee entity — represents an onboarded Urban Black employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated employee ID", example = "1")
    private Long id;

    @Schema(description = "Linked user ID from user-service", example = "101")
    private Long userId;  // From user-service

    @Column(nullable = false)
    @Schema(description = "Full legal name of the employee", example = "Rajesh Kumar")
    @JsonAlias({"employeeName", "fullName"})
    private String fullName;

    @Column(nullable = false, unique = true)
    @Schema(description = "Official email address", example = "rajesh.kumar@urbanblack.com")
    private String email;

    @Column(nullable = false, unique = true)
    @Schema(description = "Mobile number", example = "9876543210")
    @JsonAlias({"mobileNo", "mobile"})
    private String mobile;

    // ── Role ─────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'DRIVER'")
    @Schema(description = "Employee role — determines access level",
            example = "DRIVER",
            allowableValues = {
                "DRIVER", "DEPOT_MANAGER", "TRAFFIC_INSPECTOR",
                "FLEET_COORDINATOR", "CUSTOMER_SUPPORT", "ACCOUNTS_OFFICER",
                "IT_ADMINISTRATOR", "ZONE_SUPERVISOR", "HR_MANAGER", "OPERATIONS_ADMIN"
            })
    @JsonAlias({"employeeRole", "role"})
    private EmployeeRole role = EmployeeRole.DRIVER;

    // ── Account Status ────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Account status", example = "ACTIVE")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column
    @Schema(description = "Verification status of employee documents", example = "PENDING_VERIFICATION")
    private com.urbanblack.common.enums.VerificationStatus verificationStatus = com.urbanblack.common.enums.VerificationStatus.PENDING_VERIFICATION;

    @Column
    @Schema(description = "Date when the employee was registered", example = "2024-03-06")
    private java.time.LocalDate registrationDate;

    @Column
    @Schema(description = "Years of experience", example = "5")
    private String experience;

    @Column
    @Schema(description = "Date of Birth", example = "1990-01-01")
    private java.time.LocalDate dateOfBirth;

    @Column
    @Schema(description = "Pin Code", example = "411038")
    private String pincode;
    
    @Column
    @Schema(description = "Medical Insurance Number", example = "MED123456")
    private String medicalInsuranceNumber;


    // ── Auto-generated Login Credentials ─────────────────────────────────────

    // These are generated on employee creation and emailed to the employee.
    // The password stored here is the plain-text temp password (for email only).
    // The hashed version is stored in user-service after registration.

    @Column
    @Schema(description = "Auto-generated username for employee login", example = "rajesh.kumar@urbanblack.in")
    private String username;

    @Column
    @Schema(description = "Auto-generated temporary password (sent via email)", example = "UB@48392")
    private String tempPassword;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Schema(description = "True once credentials have been emailed to the employee", example = "false")
    private boolean credentialsSent = false;

    // ── Document Relations ────────────────────────────────────────────────────

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private Aadhaar aadhaar;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private DrivingLicense drivingLicense;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private EmployeeEducation education;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private BankDetails bankDetails;

    @PrePersist
    @PreUpdate
    public void syncAadhaarFields() {
        if (this.aadhaar != null) {
            if (this.dateOfBirth == null && this.aadhaar.getDateOfBirth() != null) {
                this.dateOfBirth = this.aadhaar.getDateOfBirth();
            }
            if (this.pincode == null && this.aadhaar.getAddress() != null && this.aadhaar.getAddress().getPin() != null) {
                this.pincode = this.aadhaar.getAddress().getPin();
            }
        }
    }
}
