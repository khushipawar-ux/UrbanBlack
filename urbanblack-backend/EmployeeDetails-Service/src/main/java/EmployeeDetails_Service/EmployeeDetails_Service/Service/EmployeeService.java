package EmployeeDetails_Service.EmployeeDetails_Service.Service;

import EmployeeDetails_Service.EmployeeDetails_Service.client.NotificationClient;
import EmployeeDetails_Service.EmployeeDetails_Service.Entity.Employee;
import EmployeeDetails_Service.EmployeeDetails_Service.Entity.EmployeePackage;
import EmployeeDetails_Service.EmployeeDetails_Service.REPO.EmployeeRepository;
import EmployeeDetails_Service.EmployeeDetails_Service.REPO.EmployeePackageRepository;
import com.urbanblack.common.dto.EmployeeEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeePackageRepository employeePackageRepository;
    private final CredentialMailService credentialMailService;
    private final NotificationClient notificationClient;
    private final EmployeeDetails_Service.EmployeeDetails_Service.client.AuthServiceClient authServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "employee-events";

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    public Employee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }

    public List<Employee> getByRole(String role) {
        try {
            EmployeeDetails_Service.EmployeeDetails_Service.Entity.enums.EmployeeRole enumRole = 
                EmployeeDetails_Service.EmployeeDetails_Service.Entity.enums.EmployeeRole.valueOf(role.toUpperCase());
            return employeeRepository.findByRole(enumRole);
        } catch (IllegalArgumentException e) {
            log.error("Invalid role name: {}", role);
            return List.of();
        }
    }

    public List<Employee> getAllDepotManagers() {
        return employeeRepository.findByRole(EmployeeDetails_Service.EmployeeDetails_Service.Entity.enums.EmployeeRole.DEPOT_MANAGER);
    }

    // ── Create (with auto credentials + email) ────────────────────────────────

    public Employee save(Employee employee) {
        // ── Normalization ──
        if (employee.getFullName() != null) employee.setFullName(employee.getFullName().trim());
        if (employee.getEmail() != null) employee.setEmail(employee.getEmail().trim().toLowerCase());
        if (employee.getMobile() != null) employee.setMobile(employee.getMobile().trim());

        // ── Mandatory Field Check ──
        if (employee.getFullName() == null || employee.getFullName().isBlank()) {
            log.error("Employee name is missing. Cannot save.");
            throw new IllegalArgumentException("Full Name is mandatory");
        }
        if (employee.getMobile() == null || employee.getMobile().isBlank()) {
            log.error("Employee mobile is missing. Cannot save.");
            throw new IllegalArgumentException("Mobile number is mandatory");
        }
        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            log.error("Employee email is missing. Cannot save.");
            throw new IllegalArgumentException("Email is mandatory");
        }

        // ── Duplicate Check ──
        if (employee.getEmail() != null) {
            java.util.Optional<Employee> existing = employeeRepository.findByEmail(employee.getEmail());
            if (existing.isPresent()) {
                log.error("Employee with email {} already exists.", employee.getEmail());
                throw new RuntimeException("An employee with this email already exists: " + employee.getEmail());
            }
        }
        if (employee.getMobile() != null) {
            java.util.Optional<Employee> existing = employeeRepository.findByMobile(employee.getMobile());
            if (existing.isPresent()) {
                log.error("Employee with mobile {} already exists.", employee.getMobile());
                throw new RuntimeException("An employee with this mobile number already exists: " + employee.getMobile());
            }
        }

        // Set bidirectional relationships
        setRelationships(employee);

        // Auto-generate username and temp password if not already set
        // USER REQUEST: Username = Email, Password = password@123
        if (employee.getId() == null) {
            // FORCE username to always be the email as per user request
            String email = employee.getEmail();
            employee.setUsername(email);
            
            if (employee.getTempPassword() == null || employee.getTempPassword().isBlank()) {
                employee.setTempPassword(generateTempPassword());
            }
            
            employee.setCredentialsSent(false);
            log.info("[VERIFY-LOG] Provisioned new employee '{}' with username/email: {}", employee.getFullName(), email);
        }

        // Default status
        if (employee.getAccountStatus() == null) {
            employee.setAccountStatus(com.urbanblack.common.enums.AccountStatus.ACTIVE);
        }

        if (employee.getVerificationStatus() == null) {
            employee.setVerificationStatus(com.urbanblack.common.enums.VerificationStatus.PENDING_VERIFICATION);
        }


        Employee saved = employeeRepository.save(employee);

        // ── 1. Sync with Auth Service (CRITICAL for login) ───────────────────────
        try {
            log.info("Syncing credentials for employee '{}' with Auth Service", saved.getFullName());
            com.urbanblack.common.dto.request.UserRegistrationRequest authRequest = 
                com.urbanblack.common.dto.request.UserRegistrationRequest.builder()
                    .name(saved.getFullName())
                    .email(saved.getEmail())
                    .mobile(saved.getMobile())
                    .password(saved.getTempPassword())
                    .role(saved.getRole() != null ? saved.getRole().name() : "DRIVER")
                    .build();

            authServiceClient.onboardDriver(authRequest, "ADMIN"); 
            log.info("✅ Auth Service sync successful for {}", saved.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to sync with Auth Service for {}: {}", saved.getEmail(), e.getMessage());
        }

        // ── 2. Send credentials email (via Notification Service) ────────────────
        if (!saved.isCredentialsSent()) {
            try {
                log.info("Calling Notification Service to send credentials for {}", saved.getEmail());
                
                // Fetch LATEST Package Details
                EmployeePackage empPackage = employeePackageRepository.findFirstByOrderByIdDesc()
                        .orElse(EmployeePackage.builder()
                                .designation("Driver Trainee")
                                .durationMonths(6)
                                .inHandSalary(24000.0)
                                .monthlyOff(4)
                                .build());

                // Ensure username is not null for notification service
                String username = (saved.getUsername() != null && !saved.getUsername().isEmpty()) 
                        ? saved.getUsername() : saved.getEmail();

                EmployeeDetails_Service.EmployeeDetails_Service.dto.CredentialEmailRequest notifyRequest = 
                    EmployeeDetails_Service.EmployeeDetails_Service.dto.CredentialEmailRequest.builder()
                        .email(saved.getEmail())
                        .fullName(saved.getFullName())
                        .role(saved.getRole() != null ? saved.getRole().name() : "DRIVER")
                        .username(username)
                        .tempPassword(saved.getTempPassword())
                        .employeeId(String.valueOf(saved.getId()))
                        .designation(empPackage.getDesignation())
                        .durationMonths(empPackage.getDurationMonths())
                        .inHandSalary(empPackage.getInHandSalary())
                        .monthlyOff(empPackage.getMonthlyOff())
                        .medicalInsuranceNumber(saved.getMedicalInsuranceNumber())
                        .build();

                notificationClient.sendEmployeeCredentials(notifyRequest);
                
                saved.setCredentialsSent(true);
                employeeRepository.save(saved);
                log.info("✅ Notification service call successful for {}", saved.getEmail());
            } catch (Exception e) {
                log.error("❌ Failed to send credentials via Notification Service for {}: {}", saved.getEmail(), e.getMessage());
                // Fallback to internal mail service
                try {
                    credentialMailService.sendCredentials(saved);
                } catch (Exception mailEx) {
                    log.error("❌ Final email failure for {}: {}", saved.getEmail(), mailEx.getMessage());
                }
            }
        }

        broadcastEvent(saved);

        return saved;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public Employee update(Long id, Employee employeeDetails) {
        Employee employee = getById(id);

        employee.setFullName(employeeDetails.getFullName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setMobile(employeeDetails.getMobile());
        employee.setAccountStatus(employeeDetails.getAccountStatus());
        employee.setVerificationStatus(employeeDetails.getVerificationStatus());
        employee.setDateOfBirth(employeeDetails.getDateOfBirth());
        employee.setPincode(employeeDetails.getPincode());
        employee.setMedicalInsuranceNumber(employeeDetails.getMedicalInsuranceNumber());


        if (employeeDetails.getRole() != null) {
            employee.setRole(employeeDetails.getRole());
        }

        // Update document relationships
        if (employeeDetails.getAadhaar() != null) {
            employeeDetails.getAadhaar().setEmployee(employee);
            employee.setAadhaar(employeeDetails.getAadhaar());
        }
        if (employeeDetails.getDrivingLicense() != null) {
            employeeDetails.getDrivingLicense().setEmployee(employee);
            employee.setDrivingLicense(employeeDetails.getDrivingLicense());
        }
        if (employeeDetails.getEducation() != null) {
            employeeDetails.getEducation().setEmployee(employee);
            employee.setEducation(employeeDetails.getEducation());
        }
        if (employeeDetails.getBankDetails() != null) {
            employeeDetails.getBankDetails().setEmployee(employee);
            employee.setBankDetails(employeeDetails.getBankDetails());
        }

        Employee saved = employeeRepository.save(employee);
        broadcastEvent(saved);
        return saved;
    }

    private void broadcastEvent(Employee employee) {
        try {
            EmployeeEventDTO event = EmployeeEventDTO.builder()
                    .id(employee.getId())
                    .name(employee.getFullName())
                    .status(employee.getAccountStatus() != null ? employee.getAccountStatus().name() : "ACTIVE")
                    .role(employee.getRole() != null ? employee.getRole().name() : null)
                    .email(employee.getEmail())
                    .mobile(employee.getMobile())
                    .licenseNumber(
                            employee.getDrivingLicense() != null
                                    ? employee.getDrivingLicense().getLicenseNumber()
                                    : null)
                    .dateOfJoining(employee.getRegistrationDate())
                    .profileImage(
                            employee.getAadhaar() != null
                                    ? employee.getAadhaar().getProfileImage()
                                    : null)
                    .build();

            kafkaTemplate.send(TOPIC, event);
            log.info("Broadcasted employee event for: {} (role={})", employee.getFullName(), event.getRole());
        } catch (Exception e) {
            log.error("Failed to broadcast employee event", e);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    @Transactional
    public void deleteBulk(List<Long> ids) {
        employeeRepository.deleteAllById(ids);
    }

    @Transactional
    public List<Employee> createBulk(List<Employee> employees) {
        return employees.stream()
                .map(this::save)
                .toList();
    }

    @Transactional
    public Employee deactivate(Long id) {
        Employee employee = getById(id);
        employee.setAccountStatus(com.urbanblack.common.enums.AccountStatus.INACTIVE);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee activate(Long id) {
        Employee employee = getById(id);
        employee.setAccountStatus(com.urbanblack.common.enums.AccountStatus.ACTIVE);
        return employeeRepository.save(employee);
    }

    // ── Helper: set bidirectional relations ───────────────────────────────────

    private void setRelationships(Employee employee) {
        if (employee.getAadhaar() != null)         employee.getAadhaar().setEmployee(employee);
        if (employee.getDrivingLicense() != null)  employee.getDrivingLicense().setEmployee(employee);
        if (employee.getEducation() != null)        employee.getEducation().setEmployee(employee);
        if (employee.getBankDetails() != null)      employee.getBankDetails().setEmployee(employee);
    }

    // ── Credential Generation ─────────────────────────────────────────────────


    /**
     * Generates a secure 8-character temporary password.
     * Format: UB@XXXXX  (prefix + 5 alphanumeric chars)
     * Example: UB@k8mN3
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("UB@");
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public List<Employee> importEmployees(MultipartFile file) {
        System.out.println(">>> ENTERING importEmployees with file: " + file.getOriginalFilename());
        log.info("Starting CSV import for employees. File: {}", file.getOriginalFilename());
        List<Employee> importedEmployees = new ArrayList<>();
        
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, 
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            System.out.println(">>> CSV Headers Detected: " + (headerMap != null ? headerMap.keySet() : "NONE"));

            List<CSVRecord> csvRecords = csvParser.getRecords();
            System.out.println(">>> Total records read from CSV: " + csvRecords.size());

            for (CSVRecord csvRecord : csvRecords) {
                try {
                    // Supporting both camelCase, display names, and common variations
                    String name = getValue(csvRecord, "employeeName", "Employee Name", "fullName", "Full Name");
                    String email = getValue(csvRecord, "email", "Email", "Email Address");
                    String phone = getValue(csvRecord, "mobileNo", "Phone", "mobile", "Mobile No", "Phone NO");
                    String roleStr = getValue(csvRecord, "employeeRole", "Role", "role");

                    if (name == null || email == null || phone == null) {
                        System.err.println(">>> Skipping record: missing mandatory data (name/email/phone)");
                        continue;
                    }

                    Employee employee = new Employee();
                    employee.setFullName(name);
                    employee.setEmail(email);
                    employee.setMobile(phone);
                    
                    if (roleStr != null && !roleStr.isBlank()) {
                        try {
                            String enumName = roleStr.toUpperCase().trim().replace(" ", "_");
                            employee.setRole(EmployeeDetails_Service.EmployeeDetails_Service.Entity.enums.EmployeeRole.valueOf(enumName));
                        } catch (IllegalArgumentException e) {
                            employee.setRole(EmployeeDetails_Service.EmployeeDetails_Service.Entity.enums.EmployeeRole.DRIVER);
                        }
                    }

                    // Save individually (this uses our skip-duplicates logic in save())
                    Employee saved = this.save(employee);
                    importedEmployees.add(saved);
                    System.out.println(">>> Successfully processed employee: " + saved.getFullName());
                } catch (Exception e) {
                    System.err.println(">>> Failed to process record: " + e.getMessage());
                    log.error("Error processing CSV record: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
        
        return importedEmployees;
    }

    private String getValue(CSVRecord record, String... headers) {
        for (String header : headers) {
            if (record.isMapped(header) && record.get(header) != null) {
                return record.get(header).trim();
            }
        }
        return null;
    }
}
