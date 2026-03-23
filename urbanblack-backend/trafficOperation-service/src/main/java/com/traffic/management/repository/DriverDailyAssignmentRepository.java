package com.traffic.management.repository;

import com.traffic.management.entity.DriverDailyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DriverDailyAssignmentRepository extends JpaRepository<DriverDailyAssignment, Long> {
    List<DriverDailyAssignment> findByAssignmentDate(LocalDate date);
    List<DriverDailyAssignment> findByDriverId(Long driverId);
    List<DriverDailyAssignment> findByCabId(Long cabId);
    List<DriverDailyAssignment> findByDriverIdAndAssignmentDate(Long driverId, LocalDate date);
    List<DriverDailyAssignment> findByCabIdAndAssignmentDate(Long cabId, LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) > 0 FROM DriverDailyAssignment a WHERE a.driverId = :driverId AND a.status IN :statuses")
    boolean existsByDriverIdAndStatusIn(@org.springframework.data.repository.query.Param("driverId") Long driverId, @org.springframework.data.repository.query.Param("statuses") List<String> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) > 0 FROM DriverDailyAssignment a WHERE a.cabId = :cabId AND a.status IN :statuses")
    boolean existsByCabIdAndStatusIn(@org.springframework.data.repository.query.Param("cabId") Long cabId, @org.springframework.data.repository.query.Param("statuses") List<String> statuses);

    /**
     * Returns today's active (PENDING or ONGOING) assignments for a driver.
     * Used by the driver app after login to show the current shift details.
     */
    List<DriverDailyAssignment> findByDriverIdAndAssignmentDateAndStatusIn(
            Long driverId, LocalDate date, java.util.List<String> statuses);
    

    long countByStatus(String status);
    
    List<DriverDailyAssignment> findTop5ByOrderByAssignmentIdDesc();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) > 0 FROM DriverDailyAssignment a WHERE a.centerPoint.id = :centerPointId")
    boolean existsByCenterPointId(@org.springframework.data.repository.query.Param("centerPointId") Long centerPointId);

    @org.springframework.data.jpa.repository.Query("SELECT new com.traffic.management.dto.DepotCountReport(d.depotName, COUNT(a)) " +
           "FROM DriverDailyAssignment a JOIN a.depot d " +
           "GROUP BY d.depotName")
    List<com.traffic.management.dto.DepotCountReport> getDepotWiseAssignmentCount();
}
