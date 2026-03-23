package com.urbanblack.rideservice.repository;

import com.urbanblack.rideservice.entity.FareConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FareConfigRepository extends JpaRepository<FareConfig, Long> {

    Optional<FareConfig> findTopByOrderByIdDesc();
}

