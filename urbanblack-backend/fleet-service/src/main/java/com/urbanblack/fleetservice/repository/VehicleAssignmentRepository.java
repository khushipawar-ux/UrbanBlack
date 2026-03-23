package com.urbanblack.fleetservice.repository;

import com.urbanblack.fleetservice.entity.VehicleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleAssignmentRepository extends JpaRepository<VehicleAssignment, String> {

    Optional<VehicleAssignment> findByDriverIdAndStatus(String driverId, String status);
    Optional<VehicleAssignment> findByVehicle_IdAndStatus(String vehicleId, String status);

}
