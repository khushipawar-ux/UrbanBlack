package com.urbanblack.rideservice.repository;

import com.urbanblack.rideservice.entity.DriverKmLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DriverKmLedgerRepository extends JpaRepository<DriverKmLedger, String> {

    Optional<DriverKmLedger> findByDriverIdAndDate(String driverId, LocalDate date);

    List<DriverKmLedger> findByDriverIdAndDateBetweenOrderByDateDesc(String driverId, LocalDate from, LocalDate to);

    List<DriverKmLedger> findByDateBetweenOrderByDateDesc(LocalDate from, LocalDate to);
}

