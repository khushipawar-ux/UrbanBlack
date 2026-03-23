package com.traffic.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.traffic.management.entity.FareSlab;

@Repository
public interface FareSlabRepository extends JpaRepository<FareSlab, Long> {

    @Query("""
        SELECT f FROM FareSlab f 
        WHERE :distance BETWEEN f.minDistance AND f.maxDistance
        AND f.isActive = true
    """)
    FareSlab findByDistance(@Param("distance") Double distance);
}