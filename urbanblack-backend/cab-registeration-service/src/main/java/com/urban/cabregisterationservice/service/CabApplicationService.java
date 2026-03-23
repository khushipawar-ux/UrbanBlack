package com.urban.cabregisterationservice.service;

import com.urban.cabregisterationservice.dto.ApplicationStatusResponse;
import com.urban.cabregisterationservice.dto.CabApplicationRequest;
import com.urban.cabregisterationservice.dto.UpdateCabApplicationRequest;
import com.urban.cabregisterationservice.entity.CabApplication;
import com.urban.cabregisterationservice.entity.RcDetails;
import com.urban.cabregisterationservice.repository.CabApplicationRepository;
import com.urban.cabregisterationservice.repository.RcDetailsRepository;
import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.common.enums.ApplicationStage;
import com.urbanblack.common.enums.ApplicationStatus;
import com.urbanblack.common.enums.CabCategory;
import com.urbanblack.common.enums.CabModel;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


import org.springframework.kafka.core.KafkaTemplate;
import com.urbanblack.common.dto.CabApprovedEventDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class CabApplicationService {

    private final CabApplicationRepository cabApplicationRepository;
    private final RcDetailsRepository rcDetailsRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "cab-registration-events";

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getName();
    }

    private List<CabApplication> getApplicationsForCurrentUser() {
        String email = getCurrentUserEmail();
        List<CabApplication> applications = cabApplicationRepository.findByUsername(email);
        if (applications.isEmpty()) {
            throw new BadRequestException("No applications found. Please submit application first.");
        }
        return applications;
    }

    @Transactional
    public ApiResponse newCabApplication(CabApplicationRequest cabApplicationRequest) {
        log.info("Request: {}", cabApplicationRequest);

        CabApplication cabApplication = new CabApplication();

        if (cabApplicationRequest.getYear() == null) {
            log.error("Validation failed: Vehicle year is null or invalid numeric format");
            throw new BadRequestException("Vehicle year must be a valid number and cannot be null");
        }
        int currentYear = LocalDate.now().getYear();
        int age = currentYear - cabApplicationRequest.getYear();

        // Cost Calculations based on year and kms
        if (age < 0) {
            log.error("Validation failed: Vehicle year {} is in the future", cabApplicationRequest.getYear());
            throw new IllegalArgumentException("Vehicle year cannot be in the future");
        }

        if (age <= 3) {
            cabApplication.setPackageAmount(calculateAmountBasedOnKms(cabApplicationRequest.getKms()));
        } else {
            log.error("Validation failed: Car age is {} years (max 3). Car year: {}", age, cabApplicationRequest.getYear());
            throw new IllegalArgumentException("Car older than 3 years is not valid for registration");
        }
        
        // Use user-provided package amount if available
        if (cabApplicationRequest.getPackageAmount() != null) {
            cabApplication.setPackageAmount(cabApplicationRequest.getPackageAmount());
        }

        log.info("Final Package Amount : {}", cabApplication.getPackageAmount());

        // Map CabApplication Details
        cabApplication.setCarName(cabApplicationRequest.getCarName());
        cabApplication.setCabModel(cabApplicationRequest.getCabModel());
        cabApplication.setAcType(cabApplicationRequest.getAcType());

        if (cabApplicationRequest.getCategory() != null) {
            cabApplication.setCategory(cabApplicationRequest.getCategory());
        }
        cabApplication.setUsername(getCurrentUserEmail());
        cabApplication.setVehicleYear(cabApplicationRequest.getYear());
        cabApplication.setKms(cabApplicationRequest.getKms());
        cabApplication.setPassingDate(cabApplicationRequest.getPassingDate());
        cabApplication.setFuelType(cabApplicationRequest.getFuelType());
        cabApplication.setNumberPlate(cabApplicationRequest.getCarNumber() != null ? 
                cabApplicationRequest.getCarNumber() : 
                cabApplicationRequest.getRcNumberVerification());
        
        // Status handling - match Enum name or use default
        String statusStr = cabApplicationRequest.getStatus();
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                cabApplication.setStatus(ApplicationStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}. Defaulting to PENDING_ADMIN_APPROVAL", statusStr);
                cabApplication.setStatus(ApplicationStatus.PENDING_ADMIN_APPROVAL);
            }
        } else {
            cabApplication.setStatus(ApplicationStatus.PENDING_ADMIN_APPROVAL);
        }

        // Stage handling - match Enum name or use default
        String stageStr = cabApplicationRequest.getStage();
        if (stageStr != null && !stageStr.isBlank()) {
            try {
                cabApplication.setStage(ApplicationStage.valueOf(stageStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid stage: {}. Defaulting to ROUND_0", stageStr);
                cabApplication.setStage(ApplicationStage.ROUND_0);
            }
        } else {
            cabApplication.setStage(ApplicationStage.ROUND_0);
        }

        // Map RC Details from flat request
        RcDetails rcDetail = new RcDetails();
        rcDetail.setRcNumber(cabApplicationRequest.getCarNumber() != null ? 
                cabApplicationRequest.getCarNumber() : 
                cabApplicationRequest.getRcNumberVerification());
        
        rcDetail.setVehicleChasiNumber(cabApplicationRequest.getChassisNumber());
        rcDetail.setVehicleEngineNumber(cabApplicationRequest.getEngineNumber());
        rcDetail.setVehicleModel(cabApplicationRequest.getCabModel());
        rcDetail.setFuelType(cabApplicationRequest.getFuelType());
        rcDetail.setRegistrationDate(cabApplicationRequest.getPassingDate());
        
        // Save entities
        rcDetailsRepository.save(rcDetail);
        cabApplication.setRcNumber(rcDetail);
        cabApplicationRepository.save(cabApplication);
        log.info("Saved Cab Application and Rc Details");

        return ApiResponse.builder()
                .message("New Cab Application Saved Successfully")
                .success(true)
                .build();
    }
//
    @Transactional
    public ApiResponse updateCabApplication(UpdateCabApplicationRequest updateCabApplicationRequest, Long applicationId) {
        CabApplication application = cabApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application Not Found with id : " + applicationId));

        log.info("Updating Cab Application");

        // Safe String comparison
        if ("APPROVED".equals(updateCabApplicationRequest.getUpdatedStatus())) {

            ApplicationStage currentStage = application.getStage();
            int nextIndex = currentStage.ordinal() + 1;
            ApplicationStage[] allStages = ApplicationStage.values();

            // Safety Check: Ensure we don't go out of bounds
            if (nextIndex < allStages.length) {
                ApplicationStage nextStage = allStages[nextIndex];

                // specific check for the Final stage (assuming it is called COMPLETED)
                if (nextStage == ApplicationStage.COMPLETED) {
                    // Final Stage Logic: Mark as fully APPROVED
                    application.setStage(nextStage);
                    application.setStatus(ApplicationStatus.APPROVED);
                    broadcastCabApproved(application);
                } else {
                    // Intermediate Stage Logic (including ROUND_3): Move to next, keep IN_PROGRESS
                    application.setStage(nextStage);
                    application.setStatus(ApplicationStatus.IN_PROGRESS);
                }
            } else {
                // Edge Case: Already at the last stage.
                application.setStatus(ApplicationStatus.APPROVED);
                broadcastCabApproved(application);
            }

        } else if ("REJECTED".equals(updateCabApplicationRequest.getUpdatedStatus())) {
            application.setStatus(ApplicationStatus.REJECTED);
        } else if ("ALLOCATED".equals(updateCabApplicationRequest.getUpdatedStatus())) {
            application.setStatus(ApplicationStatus.ALLOCATED);
        }

        log.info("Saving Updated Cab Application");
        cabApplicationRepository.save(application);

        return ApiResponse
                .builder()
                .message("Updated Status of CabApplication")
                .success(true)
                .build();

    }

    private void broadcastCabApproved(CabApplication application) {
        try {
            CabApprovedEventDTO event = CabApprovedEventDTO.builder()
                    .cabId(application.getCabApplicationId())
                    .carNumber(application.getRcNumber() != null ? application.getRcNumber().getRcNumber() : "N/A")
                    .ownerId(application.getCabApplicationId()) // Simplified owner reference
                    .build();
            
            kafkaTemplate.send(TOPIC, event);
            log.info("Broadcasted CabApproved event for Cab ID: {}", application.getCabApplicationId());
        } catch (Exception e) {
            log.error("Failed to broadcast CabApproved event", e);
        }
    }

    public List<ApplicationStatusResponse> getStatus() {
        List<CabApplication> applications = getApplicationsForCurrentUser();

        return applications.stream()
                .map(app -> ApplicationStatusResponse.builder()
                        .applicationId(app.getCabApplicationId())
                        .cabModel(app.getCabModel())
                        .currentStage(app.getStage().name())
                        .overallStatus(app.getStatus().name())
                        .build())
                .toList();
    }

    public List<CabApplication> getPendingCab() {
        return cabApplicationRepository.findByStatusOrderByCreatedDateDesc(
                ApplicationStatus.PENDING_ADMIN_APPROVAL
        );
    }

    public List<CabApplication> getPendingEvaluationCab() {
        return cabApplicationRepository.findByStatusOrderByCreatedDateDesc(
                ApplicationStatus.EVALUATION_SCHEDULED
        );
    }

    public List<CabApplication> getAllApplications() {
        return cabApplicationRepository.findAll();
    }

    public CabApplication getApplicationById(Long id) {
        return cabApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cab application not found with id: " + id));
    }

    @Transactional
    public ApiResponse bulkCreateApplications(List<CabApplicationRequest> requests) {
        for (CabApplicationRequest request : requests) {
            newCabApplication(request);
        }
        return ApiResponse.builder()
                .message("Bulk Cab Applications Created Successfully (" + requests.size() + " records)")
                .success(true)
                .build();
    }

    private BigDecimal calculateAmountBasedOnKms(Long kms) {

        BigDecimal BASE_PACKAGE_AMOUNT = BigDecimal.valueOf(45000);
        BigDecimal AMOUNT_REDUCED = BigDecimal.valueOf(5000);

        return switch (kms.compareTo(50000L)) {
            case 0, -1 -> { // kms <= 50000
                if (kms == 0.0f) yield BASE_PACKAGE_AMOUNT;
                else yield BASE_PACKAGE_AMOUNT.subtract(AMOUNT_REDUCED);
            }
            case 1 -> { // kms > 50000
                if (kms <= 100000f) yield BASE_PACKAGE_AMOUNT.subtract(AMOUNT_REDUCED).multiply(BigDecimal.valueOf(2));
                else yield null; // Or handle cases > 100000f as needed
            }
            default -> null; // Should not happen with valid float input
        };
    }
}
