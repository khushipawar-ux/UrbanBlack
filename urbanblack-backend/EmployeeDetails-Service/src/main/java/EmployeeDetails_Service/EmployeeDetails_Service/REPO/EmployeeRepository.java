package EmployeeDetails_Service.EmployeeDetails_Service.REPO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import EmployeeDetails_Service.EmployeeDetails_Service.Entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /** Used during username generation to check for duplicates */
    boolean existsByUsername(String username);

    /** Used for employee login by username */
    java.util.Optional<Employee> findByUsername(String username);

    /** Used to check for duplicate emails during onboarding */
    boolean existsByEmail(String email);

    /** Used to fetch existing employee by email */
    java.util.Optional<Employee> findByEmail(String email);

    /** Used to check for duplicate phone numbers during onboarding */
    boolean existsByMobile(String mobile);

    /** Used to fetch existing employee by mobile */
    java.util.Optional<Employee> findByMobile(String mobile);

    java.util.List<Employee> findByRole(EmployeeDetails_Service.EmployeeDetails_Service.Entity.enums.EmployeeRole role);
}

