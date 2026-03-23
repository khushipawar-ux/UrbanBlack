package com.urbanblack.rideservice.repository;

import com.urbanblack.rideservice.entity.RideRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RideRouteRepository extends JpaRepository<RideRoute, String> {

    Optional<RideRoute> findByRideId(String rideId);
}

