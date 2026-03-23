package EmployeeDetails_Service.EmployeeDetails_Service.REPO;

import EmployeeDetails_Service.EmployeeDetails_Service.Entity.EmployeePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeePackageRepository extends JpaRepository<EmployeePackage, Long> {
    java.util.Optional<EmployeePackage> findFirstByOrderByIdDesc();
}
