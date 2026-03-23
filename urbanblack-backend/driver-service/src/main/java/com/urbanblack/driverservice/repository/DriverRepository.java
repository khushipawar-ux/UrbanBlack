package com.urbanblack.driverservice.repository;

import com.urbanblack.driverservice.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, String> {
    Optional<Driver> findByEmail(String email);

    Optional<Driver> findByPhoneNumber(String phoneNumber);

    Optional<Driver> findByEmployeeId(String employeeId);
}
