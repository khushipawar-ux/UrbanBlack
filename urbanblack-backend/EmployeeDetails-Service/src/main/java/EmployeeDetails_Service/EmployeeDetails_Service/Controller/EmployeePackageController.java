package EmployeeDetails_Service.EmployeeDetails_Service.Controller;

import EmployeeDetails_Service.EmployeeDetails_Service.Entity.EmployeePackage;
import EmployeeDetails_Service.EmployeeDetails_Service.Service.EmployeePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee-packages")
@RequiredArgsConstructor
public class EmployeePackageController {

    private final EmployeePackageService employeePackageService;

    @PostMapping
    public ResponseEntity<EmployeePackage> createPackage(@RequestBody EmployeePackage employeePackage) {
        return ResponseEntity.ok(employeePackageService.createPackage(employeePackage));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeePackage> updatePackage(@PathVariable Long id, @RequestBody EmployeePackage employeePackage) {
        return ResponseEntity.ok(employeePackageService.updatePackage(id, employeePackage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeePackage> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(employeePackageService.getPackageById(id));
    }

    @GetMapping
    public ResponseEntity<List<EmployeePackage>> getAllPackages() {
        return ResponseEntity.ok(employeePackageService.getAllPackages());
    }
}
