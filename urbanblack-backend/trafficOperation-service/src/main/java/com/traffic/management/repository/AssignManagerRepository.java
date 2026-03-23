package com.traffic.management.repository;

import com.traffic.management.entity.AssignManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignManagerRepository extends JpaRepository<AssignManager, Long> {

    /** All assignments for a specific depot */
    List<AssignManager> findByDepotId(Long depotId);

    /** Latest assignment for a depot */
    Optional<AssignManager> findTopByDepotIdOrderByIdDesc(Long depotId);

    /** All assignments for a specific manager */
    List<AssignManager> findByManagerId(Long managerId);
}
