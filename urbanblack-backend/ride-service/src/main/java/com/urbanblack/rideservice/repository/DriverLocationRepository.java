package com.urbanblack.rideservice.repository;

import com.urbanblack.rideservice.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, String> {

    Optional<DriverLocation> findFirstByDriverIdOrderByUpdatedAtDesc(String driverId);
}

