package com.urbanblack.rideservice.repository;

import com.urbanblack.rideservice.entity.DriverShift;
import com.urbanblack.rideservice.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverShiftRepository extends JpaRepository<DriverShift, String> {

    Optional<DriverShift> findFirstByDriverIdAndStatusOrderByShiftStartDesc(String driverId, DriverStatus status);

    Optional<DriverShift> findByShiftRef(String shiftRef);
}

