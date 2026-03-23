package com.traffic.management.repository;

import com.traffic.management.entity.AllocationVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationVehicleRepository extends JpaRepository<AllocationVehicle, Long> {

    /** Find all vehicle-to-depot assignments for a specific depot */
    List<AllocationVehicle> findByDepotId(Long depotId);

    /** Find the latest (most recent) assignment for a depot – ordered by id descending */
    java.util.Optional<AllocationVehicle> findTopByDepotIdOrderByIdDesc(Long depotId);
}
