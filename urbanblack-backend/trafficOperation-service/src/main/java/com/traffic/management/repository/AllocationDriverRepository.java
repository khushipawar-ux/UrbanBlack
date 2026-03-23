package com.traffic.management.repository;

import com.traffic.management.entity.AllocationDriver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationDriverRepository extends JpaRepository<AllocationDriver, Long> {

    /** Find all driver-to-depot assignments for a specific depot */
    List<AllocationDriver> findByDepotId(Long depotId);

    /** Find the latest (most recent) assignment for a depot – ordered by id descending */
    java.util.Optional<AllocationDriver> findTopByDepotIdOrderByIdDesc(Long depotId);
}
