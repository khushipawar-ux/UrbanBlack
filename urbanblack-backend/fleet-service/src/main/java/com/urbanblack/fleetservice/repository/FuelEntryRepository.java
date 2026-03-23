package com.urbanblack.fleetservice.repository;

import com.urbanblack.fleetservice.entity.FuelEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuelEntryRepository extends JpaRepository<FuelEntry, String> {

    List<FuelEntry> findByVehicle_Id(String vehicleId);
    
    Page<FuelEntry> findByDriverIdOrderByTimestampDesc(String driverId, Pageable pageable);
    
    List<FuelEntry> findByDriverId(String driverId);
}
