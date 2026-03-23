package com.urbanblack.rideservice.repository;

import com.urbanblack.rideservice.entity.DriverKmLog;
import com.urbanblack.rideservice.entity.KmCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DriverKmLogRepository extends JpaRepository<DriverKmLog, String> {

    List<DriverKmLog> findByDriverIdAndRecordedAtBetween(String driverId, LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(l.km),0) from DriverKmLog l where l.driverId = :driverId and l.category = :category and l.recordedAt between :from and :to")
    Double sumKmByCategoryAndDateRange(@Param("driverId") String driverId,
                                       @Param("category") KmCategory category,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(l.km),0) from DriverKmLog l where l.shiftId = :shiftId and l.category = :category")
    Double sumKmByCategoryAndShift(@Param("shiftId") String shiftId,
                                  @Param("category") KmCategory category);
}

