package com.traffic.management.repository;

import com.traffic.management.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Long> {
    Page<Depot> findByCityContainingIgnoreCase(String city, Pageable pageable);
    Page<Depot> findByIsActive(Boolean isActive, Pageable pageable);
    Page<Depot> findByDepotNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrDepotCodeContainingIgnoreCase(String name, String city, String code, Pageable pageable);
}
