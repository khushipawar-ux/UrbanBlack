package com.urbanblack.driverservice.service;

import com.urbanblack.driverservice.client.RideServiceClient;
import com.urbanblack.driverservice.dto.*;
import com.urbanblack.driverservice.entity.Driver;
import com.urbanblack.driverservice.entity.Shift;
import com.urbanblack.driverservice.repository.DriverRepository;
import com.urbanblack.driverservice.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceReportService {

    private final DriverRepository driverRepository;
    private final ShiftRepository shiftRepository;
    private final RideServiceClient rideServiceClient;

    // Default shift start time for LATE calculation (e.g., 09:00 AM)
    private static final LocalTime DEFAULT_SHIFT_START_TIME = LocalTime.of(9, 0);

    public List<MonthlyPerformanceReportDTO> getMonthlyPerformanceData(int month, int year) {
        log.info("Generating performance data for {}/{} in Driver Service", month, year);

        // 1. Fetch all Drivers
        List<Driver> allDrivers = driverRepository.findAll();

        // 2. Fetch Attendance Records (Shifts) for the month range
        LocalDate startMonthDate = LocalDate.of(year, month, 1);
        LocalDateTime monthStart = startMonthDate.atStartOfDay();
        LocalDateTime monthEnd = startMonthDate.withDayOfMonth(startMonthDate.lengthOfMonth()).atTime(LocalTime.MAX);
        
        List<Shift> shifts = shiftRepository.findAllByClockInTimeBetween(monthStart, monthEnd);

        // 3. Group present days and online hours by driverId
        Map<String, Long> presentDaysMap = shifts.stream()
                .filter(s -> calculateAttendanceStatus(s) == AttendanceStatus.PRESENT)
                .collect(Collectors.groupingBy(Shift::getDriverId, Collectors.counting()));

        Map<String, Double> onlineHoursMap = shifts.stream()
                .collect(Collectors.groupingBy(
                        Shift::getDriverId,
                        Collectors.summingDouble(s -> s.getTotalActiveMinutes() / 60.0)
                ));

        // 4. Fetch Ride Performance from Ride Service via Feign
        List<MonthlyRideSummaryDTO> rideSummaries = new ArrayList<>();
        try {
            rideSummaries = rideServiceClient.getMonthlySummary(month, year);
        } catch (Exception e) {
            log.error("Error fetching ride summaries: {}", e.getMessage());
        }
        
        Map<String, MonthlyRideSummaryDTO> rideSummaryMap = rideSummaries.stream()
                .collect(Collectors.toMap(MonthlyRideSummaryDTO::getDriverId, rs -> rs, (p1, p2) -> p1));

        // 5. Assemble DTOs
        List<MonthlyPerformanceReportDTO> report = new ArrayList<>();
        for (Driver driver : allDrivers) {
            String driverId = driver.getId();

            Long presentDays = presentDaysMap.getOrDefault(driverId, 0L);
            Double onlineHours = onlineHoursMap.getOrDefault(driverId, 0.0);
            
            Long rideCount = 0L;
            Double totalKm = 0.0;

            if (rideSummaryMap.containsKey(driverId)) {
                MonthlyRideSummaryDTO rs = rideSummaryMap.get(driverId);
                rideCount = rs.getRideCount();
                totalKm = rs.getTotalKm();
            }

            // Estimated Salary Calculation (Consistent with EmployeeDetails-Service)
            double base = presentDays >= 22 ? 15000.0 : (presentDays * 500.0);
            double estimatedSalary = base + (rideCount * 50.0) + (totalKm * 2.0);

            MonthlyPerformanceReportDTO dto = MonthlyPerformanceReportDTO.builder()
                    .driverId(driverId)
                    .driverName(driver.getFirstName() + " " + driver.getLastName())
                    .empId(driver.getEmployeeId())
                    .depotName(driver.getDepotName() != null ? driver.getDepotName() : "N/A")
                    .presentDays(presentDays)
                    .totalRides(rideCount)
                    .totalKm(totalKm)
                    .avgRating(driver.getRating() != null ? driver.getRating() : 0.0)
                    .onlineHours(onlineHours)
                    .estimatedSalary(estimatedSalary)
                    .build();

            report.add(dto);
        }

        return report;
    }

    public DriverDetailedSummaryDTO getDriverMonthlySummary(String driverId, int month, int year) {
        log.info("Generating detailed summary for driver {} in {}/{}", driverId, month, year);

        // 1. Fetch Driver (driverId may be UUID or employeeId – admin assigns by employeeId)
        Driver driver = driverRepository.findById(driverId)
                .or(() -> driverRepository.findByEmployeeId(driverId))
                .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

        String resolvedDriverId = driver.getId(); // Shift.driverId uses internal UUID

        // 2. Fetch Performance Data (Reuse existing logic or slim it down)
        // For simplicity and consistency, I'll filter the aggregate report or just calculate for this driver.
        // Let's just calculate for this driver to be efficient.

        LocalDate startMonthDate = LocalDate.of(year, month, 1);
        LocalDateTime monthStart = startMonthDate.atStartOfDay();
        LocalDateTime monthEnd = startMonthDate.withDayOfMonth(startMonthDate.lengthOfMonth()).atTime(LocalTime.MAX);

        List<Shift> driverShifts = shiftRepository.findAllByClockInTimeBetween(monthStart, monthEnd).stream()
                .filter(s -> s.getDriverId().equals(resolvedDriverId))
                .collect(Collectors.toList());

        long presentDays = driverShifts.stream()
                .filter(s -> calculateAttendanceStatus(s) == AttendanceStatus.PRESENT)
                .count();

        double onlineHours = driverShifts.stream()
                .mapToDouble(s -> s.getTotalActiveMinutes() / 60.0)
                .sum();

        // Fetch Ride Performance for this driver (ride-service uses employeeId as driverId)
        String rideDriverId = driver.getEmployeeId() != null && !driver.getEmployeeId().isBlank()
                ? driver.getEmployeeId() : driver.getId();
        MonthlyRideSummaryDTO rideStats = null;
        try {
            List<MonthlyRideSummaryDTO> summaries = rideServiceClient.getMonthlySummary(month, year);
            rideStats = summaries.stream()
                    .filter(s -> rideDriverId.equals(s.getDriverId()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error fetching ride summaries: {}", e.getMessage());
        }

        Long rideCount = rideStats != null ? rideStats.getRideCount() : 0L;
        Double totalKm = rideStats != null && rideStats.getTotalKm() != null ? rideStats.getTotalKm() : 0.0;

        double base = presentDays >= 22 ? 15000.0 : (presentDays * 500.0);
        double estimatedSalary = base + (rideCount * 50.0) + (totalKm * 2.0);

        MonthlyPerformanceReportDTO summary = MonthlyPerformanceReportDTO.builder()
                .driverId(driver.getId())
                .driverName(driver.getFirstName() + " " + driver.getLastName())
                .empId(driver.getEmployeeId())
                .depotName(driver.getDepotName() != null ? driver.getDepotName() : "N/A")
                .presentDays(presentDays)
                .totalRides(rideCount)
                .totalKm(totalKm)
                .avgRating(driver.getRating() != null ? driver.getRating() : 0.0)
                .onlineHours(onlineHours)
                .estimatedSalary(estimatedSalary)
                .build();

        // 3. Map Detailed Logs
        List<AttendanceResponseDTO> dailyLogs = driverShifts.stream()
                .map(this::mapToAttendanceDTO)
                .collect(Collectors.toList());

        return DriverDetailedSummaryDTO.builder()
                .summary(summary)
                .dailyLogs(dailyLogs)
                .build();
    }

    private AttendanceResponseDTO mapToAttendanceDTO(Shift shift) {
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

    public List<DriverMonthlyHoursDTO> getMonthlyHoursData(int month, int year) {
        log.info("Generating daily hours data for {}/{} in Driver Service", month, year);

        // 1. Fetch all Drivers
        List<Driver> allDrivers = driverRepository.findAll();

        // 2. Fetch Attendance Records for the month range
        LocalDate startMonthDate = LocalDate.of(year, month, 1);
        LocalDateTime monthStart = startMonthDate.atStartOfDay();
        LocalDateTime monthEnd = startMonthDate.withDayOfMonth(startMonthDate.lengthOfMonth()).atTime(LocalTime.MAX);

        List<Shift> shifts = shiftRepository.findAllByClockInTimeBetween(monthStart, monthEnd);

        // 3. Group shifts by driverId and date
        Map<String, Map<LocalDate, Shift>> driverShiftsMap = shifts.stream()
                .collect(Collectors.groupingBy(
                        Shift::getDriverId,
                        Collectors.toMap(
                                s -> s.getClockInTime().toLocalDate(),
                                s -> s,
                                (s1, s2) -> s1 // In case of more than one shift, pick the first
                        )
                ));

        int daysInMonth = startMonthDate.lengthOfMonth();
        List<DriverMonthlyHoursDTO> result = new ArrayList<>();

        for (Driver driver : allDrivers) {
            String driverId = driver.getId();
            Map<Integer, Double> dailyHours = new java.util.HashMap<>();
            Map<LocalDate, Shift> driverDailyShifts = driverShiftsMap.getOrDefault(driverId, Map.of());

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = LocalDate.of(year, month, day);
                Shift shift = driverDailyShifts.get(currentDate);

                double hours = 0.0;
                if (shift != null) {
                    hours = shift.getTotalActiveMinutes() / 60.0;
                    // Round to 1 decimal place
                    hours = Math.round(hours * 10.0) / 10.0;
                }
                dailyHours.put(day, hours);
            }

            result.add(DriverMonthlyHoursDTO.builder()
                    .driverName(driver.getFirstName() + " " + driver.getLastName())
                    .empId(driver.getEmployeeId())
                    .hours(dailyHours)
                    .build());
        }

        return result;
    }

    public List<DriverMonthlyCalendarDTO> getMonthlyCalendarAttendance(int month, int year) {
        log.info("Generating calendar attendance data for {}/{} in Driver Service", month, year);

        // 1. Fetch all Drivers
        List<Driver> allDrivers = driverRepository.findAll();

        // 2. Fetch Attendance Records for the month range
        LocalDate startMonthDate = LocalDate.of(year, month, 1);
        LocalDateTime monthStart = startMonthDate.atStartOfDay();
        LocalDateTime monthEnd = startMonthDate.withDayOfMonth(startMonthDate.lengthOfMonth()).atTime(LocalTime.MAX);
        
        List<Shift> shifts = shiftRepository.findAllByClockInTimeBetween(monthStart, monthEnd);

        // 3. Group shifts by driverId and date
        Map<String, Map<LocalDate, Shift>> driverShiftsMap = shifts.stream()
                .collect(Collectors.groupingBy(
                        Shift::getDriverId,
                        Collectors.toMap(
                                s -> s.getClockInTime().toLocalDate(),
                                s -> s,
                                (s1, s2) -> s1 // In case of more than one shift, pick the first
                        )
                ));

        int daysInMonth = startMonthDate.lengthOfMonth();
        List<DriverMonthlyCalendarDTO> result = new ArrayList<>();

        for (Driver driver : allDrivers) {
            String driverId = driver.getId();
            Map<Integer, AttendanceStatus> dailyAttendance = new java.util.HashMap<>();
            Map<LocalDate, Shift> driverDailyShifts = driverShiftsMap.getOrDefault(driverId, Map.of());

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = LocalDate.of(year, month, day);
                Shift shift = driverDailyShifts.get(currentDate);

                AttendanceStatus status;
                if (shift != null) {
                    status = calculateAttendanceStatus(shift);
                } else {
                    // Logic: Mark as ABSENT. 
                    // Future: check holidays/leaves. For now, Sunday = DAY_OFF as placeholder.
                    if (currentDate.getDayOfWeek().getValue() == 7) {
                        status = AttendanceStatus.DAY_OFF;
                    } else {
                        status = AttendanceStatus.ABSENT;
                    }
                }
                dailyAttendance.put(day, status);
            }

            result.add(DriverMonthlyCalendarDTO.builder()
                    .driverName(driver.getFirstName() + " " + driver.getLastName())
                    .empId(driver.getEmployeeId())
                    .depotName(driver.getDepotName() != null ? driver.getDepotName() : "N/A")
                    .attendance(dailyAttendance)
                    .build());
        }

        return result;
    }

    public AttendanceStatus calculateAttendanceStatus(Shift shift) {
        if (shift.getClockInTime() == null) {
            return AttendanceStatus.ABSENT;
        }

        // 1. If worked more than 12 hours, definitely PRESENT (likely over-time)
        if (shift.getTotalActiveMinutes() >= 720) {
            return AttendanceStatus.PRESENT;
        }

        // 2. Full-day threshold: 8 hours (480 minutes)
        if (shift.getTotalActiveMinutes() >= 480) {
            return AttendanceStatus.PRESENT;
        }

        // 3. Half-day threshold: 4 hours (240 minutes)
        if (shift.getTotalActiveMinutes() >= 240) {
            return AttendanceStatus.HALF_DAY;
        }

        // 4. Late check
        if (shift.getClockInTime().toLocalTime().isAfter(DEFAULT_SHIFT_START_TIME)) {
            return AttendanceStatus.LATE;
        }

        // 5. If they just clocked in but have almost no minutes, still present if early
        return AttendanceStatus.PRESENT;
    }
}
