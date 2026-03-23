package com.urbanblack.rideservice.repository;

import com.urbanblack.rideservice.entity.Ride;
import com.urbanblack.rideservice.entity.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, String> {

    Optional<Ride> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(String userId, List<RideStatus> statuses);

    Optional<Ride> findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(String driverId, List<RideStatus> statuses);

    Page<Ride> findByUserIdOrderByRequestedAtDesc(String userId, Pageable pageable);

    Page<Ride> findByDriverIdAndRequestedAtBetweenOrderByRequestedAtDesc(String driverId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Ride> findByStatusAndRequestedAtBetweenOrderByRequestedAtDesc(RideStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Ride> findByRequestedAtBetweenOrderByRequestedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT new com.urbanblack.rideservice.dto.MonthlyRideSummary(r.driverId, COUNT(r), SUM(r.rideKm)) " +
            "FROM Ride r WHERE r.status = com.urbanblack.rideservice.entity.RideStatus.RIDE_COMPLETED AND r.createdAt BETWEEN :from AND :to GROUP BY r.driverId")
    List<com.urbanblack.rideservice.dto.MonthlyRideSummary> getMonthlySummary(@org.springframework.data.repository.query.Param("from") LocalDateTime from, @org.springframework.data.repository.query.Param("to") LocalDateTime to);

    long countByStatusAndCreatedAtBetween(RideStatus status, LocalDateTime start, LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(r.fare) FROM Ride r WHERE r.status = :status AND r.createdAt BETWEEN :start AND :end")
    java.math.BigDecimal sumFareByStatusAndCreatedAtBetween(@org.springframework.data.repository.query.Param("status") RideStatus status, 
                                                            @org.springframework.data.repository.query.Param("start") LocalDateTime start, 
                                                            @org.springframework.data.repository.query.Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(r.fare), 0) FROM Ride r WHERE r.status = :status")
    java.math.BigDecimal sumFareByStatus(@org.springframework.data.repository.query.Param("status") RideStatus status);

    long countByUserIdAndStatus(String userId, RideStatus status);

    java.util.Optional<Ride> findTopByUserIdAndStatusOrderByCreatedAtDesc(String userId, RideStatus status);
}

