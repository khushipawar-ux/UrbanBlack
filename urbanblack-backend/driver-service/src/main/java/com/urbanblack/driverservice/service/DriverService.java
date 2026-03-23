package com.urbanblack.driverservice.service;

import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.driverservice.dto.DriverSummaryDto;
import com.urbanblack.driverservice.entity.Driver;
import com.urbanblack.driverservice.entity.Shift;
import com.urbanblack.driverservice.entity.ShiftStatus;
import com.urbanblack.driverservice.repository.DriverRepository;
import com.urbanblack.driverservice.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DriverService {

        private final DriverRepository driverRepository;
        private final ShiftRepository shiftRepository;

        public ApiResponse<Driver> getProfile(String email) {
                Driver driver = driverRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Driver not found"));

                return ApiResponse.<Driver>builder()
                                .success(true)
                                .data(driver)
                                .build();
        }

        public ApiResponse<Driver> updateProfile(String email, Driver updated) {
                Driver driver = driverRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Driver not found"));

                driver.setFirstName(updated.getFirstName());
                driver.setLastName(updated.getLastName());
                driver.setPhoneNumber(updated.getPhoneNumber());
                driver.setLanguage(updated.getLanguage());
                driver.setUpdatedAt(LocalDateTime.now());

                driverRepository.save(driver);

                return ApiResponse.<Driver>builder()
                                .success(true)
                                .data(driver)
                                .message("Profile updated successfully")
                                .build();
        }

        /**
         * Returns a lightweight driver summary consumed by ride-service via Feign.
         * Includes the active shift ID/status and the driver's last known coordinates.
         * driverId may be the Driver's UUID (id) or employeeId.
         */
        public DriverSummaryDto getDriverSummary(String driverId) {
                Driver driver = driverRepository.findById(driverId)
                                .or(() -> driverRepository.findByEmployeeId(driverId))
                                .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

                String resolvedId = driver.getId();
                Shift activeShift = shiftRepository
                                .findByDriverIdAndStatus(resolvedId, ShiftStatus.ACTIVE)
                                .orElse(null);

                return DriverSummaryDto.builder()
                                .driverId(resolvedId)
                                .employeeId(driver.getEmployeeId())
                                .firstName(driver.getFirstName())
                                .lastName(driver.getLastName())
                                .shiftId(activeShift != null ? activeShift.getId() : null)
                                .shiftStatus(activeShift != null ? activeShift.getStatus().name() : null)
                                .isOnline(activeShift != null
                                                && activeShift.getAvailability() != null
                                                && activeShift.getAvailability().name().equals("ONLINE"))
                                .latitude(activeShift != null ? activeShift.getClockInLatitude() : null)
                                .longitude(activeShift != null ? activeShift.getClockInLongitude() : null)
                                .build();
        }
}