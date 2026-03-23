package com.urbanblack.driverservice.repository;

import com.urbanblack.driverservice.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, String> {

    Optional<Shift> findByDriverIdAndStatus(String driverId,
                                            com.urbanblack.driverservice.entity.ShiftStatus status);

    List<Shift> findAllByClockInTimeBetween(LocalDateTime start, LocalDateTime end);
}
