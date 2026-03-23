package com.urbanblack.fleetservice.repository;

import com.urbanblack.fleetservice.entity.IssueReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueReportRepository extends JpaRepository<IssueReport, String> {
    
    List<IssueReport> findByDriverIdOrderByTimestampDesc(String driverId);
    
    Optional<IssueReport> findByTicketNumber(String ticketNumber);
}
