package com.traffic.management.repository;

import com.traffic.management.entity.ShiftMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShiftMasterRepository extends JpaRepository<ShiftMaster, Long> {
    List<ShiftMaster> findByIsActiveTrue();
}
