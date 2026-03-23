package com.urbanblack.driverservice.controller;

import com.urbanblack.driverservice.dto.AttendanceResponseDTO;
import com.urbanblack.driverservice.dto.DriverDetailedSummaryDTO;
import com.urbanblack.driverservice.dto.DriverMonthlyCalendarDTO;
import com.urbanblack.driverservice.dto.DriverMonthlyHoursDTO;
import com.urbanblack.driverservice.dto.MonthlyPerformanceReportDTO;
import com.urbanblack.driverservice.service.ExcelExportService;
import com.urbanblack.driverservice.service.PerformanceReportService;
import com.urbanblack.driverservice.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/driver/attendance")
@RequiredArgsConstructor
@Slf4j
public class AdminAttendanceController {

    private final PerformanceReportService performanceReportService;
    private final ExcelExportService excelExportService;
    private final ShiftRepository shiftRepository;

    @GetMapping
    public ResponseEntity<List<AttendanceResponseDTO>> getAllAttendance() {
        return ResponseEntity.ok(shiftRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/date")
    public ResponseEntity<List<AttendanceResponseDTO>> getAttendanceByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return ResponseEntity.ok(shiftRepository.findAllByClockInTimeBetween(startOfDay, endOfDay).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/range")
    public ResponseEntity<List<AttendanceResponseDTO>> getAttendanceByRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return ResponseEntity.ok(shiftRepository.findAllByClockInTimeBetween(start, end).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/monthly-report")
    public ResponseEntity<List<MonthlyPerformanceReportDTO>> getMonthlyReport(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        log.info("Received request for monthly report: month={}, year={}", month, year);
        return ResponseEntity.ok(performanceReportService.getMonthlyPerformanceData(month, year));
    }

    @GetMapping("/monthly-calendar")
    public ResponseEntity<List<DriverMonthlyCalendarDTO>> getMonthlyCalendar(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        log.info("Received request for monthly calendar attendance: month={}, year={}", month, year);
        return ResponseEntity.ok(performanceReportService.getMonthlyCalendarAttendance(month, year));
    }

    @GetMapping("/monthly-hours")
    public ResponseEntity<List<DriverMonthlyHoursDTO>> getMonthlyHours(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        log.info("Received request for monthly hours worked: month={}, year={}", month, year);
        return ResponseEntity.ok(performanceReportService.getMonthlyHoursData(month, year));
    }

    @GetMapping("/driver-summary/{driverId}")
    public ResponseEntity<DriverDetailedSummaryDTO> getDriverSummary(
            @PathVariable String driverId,
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        log.info("Received request for driver summary: driverId={}, month={}, year={}", driverId, month, year);
        return ResponseEntity.ok(performanceReportService.getDriverMonthlySummary(driverId, month, year));
    }

    @GetMapping("/monthly-report/export")
    public ResponseEntity<Resource> exportMonthlyReport(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        log.info("Received request to export monthly report: month={}, year={}", month, year);
        List<MonthlyPerformanceReportDTO> reportData = performanceReportService.getMonthlyPerformanceData(month, year);
        ByteArrayInputStream in = excelExportService.exportMonthlyReport(reportData);

        InputStreamResource file = new InputStreamResource(in);
        String fileName = "Driver_Performance_Report_" + year + "_" + month + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    private AttendanceResponseDTO mapToDTO(com.urbanblack.driverservice.entity.Shift shift) {
        return AttendanceResponseDTO.builder()
                .id(shift.getId())
                .driverId(shift.getDriverId())
                .status(shift.getStatus() != null ? shift.getStatus().name() : null)
                .availability(shift.getAvailability() != null ? shift.getAvailability().name() : null)
                .clockInTime(shift.getClockInTime())
                .clockInLatitude(shift.getClockInLatitude())
                .clockInLongitude(shift.getClockInLongitude())
                .clockOutTime(shift.getClockOutTime())
                .clockOutLatitude(shift.getClockOutLatitude())
                .clockOutLongitude(shift.getClockOutLongitude())
                .lastOnlineTime(shift.getLastOnlineTime())
                .lastOfflineTime(shift.getLastOfflineTime())
                .accumulatedActiveSeconds(shift.getAccumulatedActiveSeconds())
                .totalActiveMinutes(shift.getTotalActiveMinutes())
                .startingOdometer(shift.getStartingOdometer())
                .endingOdometer(shift.getEndingOdometer())
                .fuelLevelAtStart(shift.getFuelLevelAtStart() != null ? shift.getFuelLevelAtStart().name() : null)
                .fuelLevelAtEnd(shift.getFuelLevelAtEnd() != null ? shift.getFuelLevelAtEnd().name() : null)
                .vehicleCondition(shift.getVehicleCondition() != null ? shift.getVehicleCondition().name() : null)
                .build();
    }
}
