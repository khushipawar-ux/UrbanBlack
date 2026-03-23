package EmployeeDetails_Service.EmployeeDetails_Service.Controller;

import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import EmployeeDetails_Service.EmployeeDetails_Service.Entity.Employee;
import EmployeeDetails_Service.EmployeeDetails_Service.Service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "APIs for managing employee details")
public class EmployeeController {

    private final EmployeeService employeeService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeController.class);

    
	

	@PostMapping
	@Operation(
		summary = "Create a new employee",
		description = "Creates a new employee record with all associated details"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Employee created successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid employee data"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
    public ResponseEntity<Employee> createEmployee(
    		@Parameter(description = "Employee details to create", required = true)
    		@RequestBody Employee employee){
        System.out.println(">>> RECEIVED CREATE EMPLOYEE REQUEST FOR: " + (employee != null ? employee.getFullName() : "NULL"));
        return ResponseEntity.ok(employeeService.save(employee));
    }

    @GetMapping
    @Operation(summary = "Get all employees")
    public ResponseEntity<List<com.urbanblack.common.dto.employee.EmployeeResponseDTO>> getAllEmployees() {
        log.info(">>> GET ALL EMPLOYEES REQUEST RECEIVED");
        List<Employee> list = employeeService.getAll();
        log.info(">>> FOUND {} EMPLOYEES IN DB", list.size());
        return ResponseEntity.ok(list.stream()
                .map(this::mapToDTO)
                .peek(dto -> log.debug("Mapped employee: ID={}, Name={}, Role={}", dto.getId(), dto.getFullName(), dto.getRole()))
                .toList());
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get employees by role")
    public ResponseEntity<List<com.urbanblack.common.dto.employee.EmployeeResponseDTO>> getEmployeesByRole(@PathVariable String role) {
        return ResponseEntity.ok(employeeService.getByRole(role).stream()
                .map(this::mapToDTO)
                .toList());
    }

    @GetMapping("/depot-managers")
    @Operation(summary = "Get all Depot Managers")
    public ResponseEntity<List<com.urbanblack.common.dto.employee.EmployeeResponseDTO>> getDepotManagers() {
        return ResponseEntity.ok(employeeService.getAllDepotManagers().stream()
                .map(this::mapToDTO)
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(
		summary = "Get employee by ID",
		description = "Retrieves employee details by their unique identifier"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Employee found"),
		@ApiResponse(responseCode = "404", description = "Employee not found"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
    public ResponseEntity<com.urbanblack.common.dto.employee.EmployeeResponseDTO> getEmployee(
    		@Parameter(description = "Employee ID", required = true)
    		@PathVariable Long id){
        return ResponseEntity.ok(mapToDTO(employeeService.getById(id)));
    }

    private com.urbanblack.common.dto.employee.EmployeeResponseDTO mapToDTO(Employee emp) {
        com.urbanblack.common.dto.employee.EmployeeResponseDTO.EmployeeResponseDTOBuilder builder = com.urbanblack.common.dto.employee.EmployeeResponseDTO.builder()
                .id(emp.getId())
                .fullName(emp.getFullName())
                .email(emp.getEmail())
                .mobile(emp.getMobile())
                .role(emp.getRole() != null ? emp.getRole().name() : null)
                .accountStatus(emp.getAccountStatus() != null ? emp.getAccountStatus().name() : null)
                .verificationStatus(emp.getVerificationStatus() != null ? emp.getVerificationStatus().name() : null)
                .registrationDate(emp.getRegistrationDate() != null ? emp.getRegistrationDate().toString() : "2024-01-01")
                .experience(emp.getExperience() != null ? emp.getExperience() : "Not Specified")
                .dob(emp.getDateOfBirth() != null ? emp.getDateOfBirth().toString() : null)
                .pincode(emp.getPincode())
                .medicalInsuranceNumber(emp.getMedicalInsuranceNumber());

        // Map Aadhaar details
        if (emp.getAadhaar() != null) {
            builder.aadharNo(emp.getAadhaar().getAadhaarNumber());
            
            if (emp.getAadhaar().getDateOfBirth() != null && emp.getDateOfBirth() == null) {
                builder.dob(emp.getAadhaar().getDateOfBirth().toString());
            }
            
            if (emp.getAadhaar().getAddress() != null) {
                EmployeeDetails_Service.EmployeeDetails_Service.Entity.AadhaarAddress addr = emp.getAadhaar().getAddress();
                String house = addr.getHouse() != null ? addr.getHouse() : "";
                String street = addr.getStreet() != null ? addr.getStreet() : "";
                String locality = addr.getLocality() != null ? addr.getLocality() : "";
                String fullAddress = (house + " " + street + " " + locality).trim();
                
                builder.address(fullAddress.isEmpty() ? null : fullAddress)
                       .city(addr.getDistrict())
                       .state(addr.getState())
                       .pincode(addr.getPin());
            }
        }

        // Map Driving License
        if (emp.getDrivingLicense() != null) {
            builder.licenseNo(emp.getDrivingLicense().getLicenseNumber());
        }

        // Map Bank Details
        if (emp.getBankDetails() != null) {
            String bankName = emp.getBankDetails().getBankName() != null ? emp.getBankDetails().getBankName() : "";
            String branchName = emp.getBankDetails().getBranchName() != null ? emp.getBankDetails().getBranchName() : "";
            builder.branchName((bankName + (branchName.isEmpty() ? "" : " - " + branchName)).trim())
                   .ifscCode(emp.getBankDetails().getIfscCode())
                   .accountNo(emp.getBankDetails().getAccountNumber());
        }

        // Map Education
        if (emp.getEducation() != null) {
            builder.recentDegree(emp.getEducation().getHighestQualification())
                   .yearOfPassing(emp.getEducation().getPassingYear())
                   .percentage(emp.getEducation().getPercentage());
        }

        return builder.build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee details")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return ResponseEntity.ok(employeeService.update(id, employee));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete employees")
    public ResponseEntity<Void> deleteEmployeesBulk(@RequestBody List<Long> ids) {
        employeeService.deleteBulk(ids);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/create-bulk")
    @Operation(summary = "Bulk create employees")
    public ResponseEntity<List<Employee>> createEmployeesBulk(@RequestBody List<Employee> employees) {
        return ResponseEntity.ok(employeeService.createBulk(employees));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate employee account")
    public ResponseEntity<Employee> deactivateEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deactivate(id));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate employee account")
    public ResponseEntity<Employee> activateEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.activate(id));
    }

    @GetMapping("/employee-details/{id}/allocations")
    @Operation(summary = "Get employee allocations")
    public ResponseEntity<List<Object>> getEmployeeAllocations(@PathVariable Long id) {
        // This is a placeholder as the allocation details might come from a different service
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/import")
    @Operation(summary = "Import employees from CSV")
    public ResponseEntity<List<Employee>> importEmployees(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(employeeService.importEmployees(file));
    }
}

