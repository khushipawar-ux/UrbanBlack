package EmployeeDetails_Service.EmployeeDetails_Service.Service;

import EmployeeDetails_Service.EmployeeDetails_Service.Entity.EmployeePackage;
import java.util.List;

public interface EmployeePackageService {
    EmployeePackage createPackage(EmployeePackage employeePackage);
    EmployeePackage updatePackage(Long id, EmployeePackage employeePackage);
    EmployeePackage getPackageById(Long id);
    List<EmployeePackage> getAllPackages();
}
