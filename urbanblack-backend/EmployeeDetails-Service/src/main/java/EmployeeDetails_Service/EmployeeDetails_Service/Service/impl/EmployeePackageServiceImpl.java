package EmployeeDetails_Service.EmployeeDetails_Service.Service.impl;

import EmployeeDetails_Service.EmployeeDetails_Service.Entity.EmployeePackage;
import EmployeeDetails_Service.EmployeeDetails_Service.REPO.EmployeePackageRepository;
import EmployeeDetails_Service.EmployeeDetails_Service.Service.EmployeePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeePackageServiceImpl implements EmployeePackageService {

    private final EmployeePackageRepository employeePackageRepository;

    @Override
    public EmployeePackage createPackage(EmployeePackage employeePackage) {
        return employeePackageRepository.save(employeePackage);
    }

    @Override
    public EmployeePackage updatePackage(Long id, EmployeePackage details) {
        EmployeePackage existing = employeePackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + id));
        
        existing.setDesignation(details.getDesignation());
        existing.setDurationMonths(details.getDurationMonths());
        existing.setInHandSalary(details.getInHandSalary());
        existing.setMonthlyOff(details.getMonthlyOff());
        
        return employeePackageRepository.save(existing);
    }

    @Override
    public EmployeePackage getPackageById(Long id) {
        return employeePackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + id));
    }

    @Override
    public List<EmployeePackage> getAllPackages() {
        return employeePackageRepository.findAll();
    }
}
