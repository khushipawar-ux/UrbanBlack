package com.urbanblack.driverservice.kafka;

import com.urbanblack.common.dto.EmployeeEventDTO;
import com.urbanblack.driverservice.entity.Driver;
import com.urbanblack.driverservice.entity.DriverStatus;
import com.urbanblack.driverservice.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Listens to 'employee-events' Kafka topic.
 * When an employee with role DRIVER is registered, automatically creates
 * a Driver record in the driver-service database.
 *
 * Only operational driver fields are persisted here.
 * Sensitive data (Aadhaar, BankDetails, Education) stays in employee-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final DriverRepository driverRepository;

    @KafkaListener(
            topics = "employee-events",
            groupId = "driver-service-group"
    )
    public void onEmployeeEvent(EmployeeEventDTO event) {
        // Only process DRIVER role events
        if (!"DRIVER".equalsIgnoreCase(event.getRole())) {
            log.debug("Skipping employee event for role={} (not a DRIVER)", event.getRole());
            return;
        }

        if (event.getEmail() == null || event.getEmail().isBlank()) {
            log.warn("Received DRIVER event without email, skipping. EmployeeId={}", event.getId());
            return;
        }

        // Idempotency: skip if already registered
        if (driverRepository.findByEmail(event.getEmail()).isPresent()) {
            log.info("Driver with email={} already exists, skipping duplicate creation.", event.getEmail());
            return;
        }

        // Split name into first / last (best-effort)
        String firstName = event.getName();
        String lastName  = null;
        if (event.getName() != null && event.getName().contains(" ")) {
            int spaceIdx = event.getName().indexOf(' ');
            firstName = event.getName().substring(0, spaceIdx).trim();
            lastName  = event.getName().substring(spaceIdx + 1).trim();
        }

        Driver driver = Driver.builder()
                .email(event.getEmail())
                .phoneNumber(event.getMobile())
                .firstName(firstName)
                .lastName(lastName)
                .profileImage(event.getProfileImage())
                .isActive("ACTIVE".equalsIgnoreCase(event.getStatus()))
                .isVerified(false)          // verified only after document review
                .licenseNumber(event.getLicenseNumber())
                .employeeId(event.getId() != null ? String.valueOf(event.getId()) : null)
                .status(DriverStatus.INACTIVE)
                .dateOfJoining(event.getDateOfJoining())
                .rating(0.0)
                .totalTrips(0)
                .totalDistance(0.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        driverRepository.save(driver);
        log.info("✅ Driver created successfully: email={}, employeeId={}", event.getEmail(), event.getId());
    }
}
