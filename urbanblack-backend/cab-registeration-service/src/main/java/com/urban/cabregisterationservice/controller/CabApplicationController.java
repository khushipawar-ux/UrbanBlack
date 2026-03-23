package com.urban.cabregisterationservice.controller;

import com.urban.cabregisterationservice.dto.*;
import com.urban.cabregisterationservice.dto.CabApplicationResponseDTO;
import com.urban.cabregisterationservice.entity.CabApplication;
import com.urban.cabregisterationservice.service.CabApplicationService;
import com.urban.cabregisterationservice.service.CabDetailsService;
import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.common.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registration")
@RequiredArgsConstructor
public class CabApplicationController {

    private final CabApplicationService cabApplicationService;
    private final CabDetailsService cabDetailsService;

    @PostMapping
    public ResponseEntity<ApiResponse> submitCabApplication(@RequestBody CabApplicationRequest cabApplication){
        return ResponseEntity.ok(cabApplicationService.newCabApplication(cabApplication));
    }

    @GetMapping("/status")
    public ResponseEntity<List<ApplicationStatusResponse>> getCabApplicationStatus() {
        return ResponseEntity.ok(cabApplicationService.getStatus());
    }

    @GetMapping("/pendingapplications")
    public List<CabApplicationResponseDTO> getPendingApplications(){
        return cabApplicationService.getPendingCab()
                .stream()
                .map(cab -> new CabApplicationResponseDTO(
                        cab.getCabApplicationId(),
                        cab.getUsername(),
                        cab.getStatus(),
                        cab.getCreatedDate()
                )).toList();
    }

    @GetMapping("/pendingapplications/evaluation")
    public List<CabApplicationResponseDTO> getPendingApplicationsForEvaluation(){
        return cabApplicationService.getPendingEvaluationCab()
                .stream()
                .map(cab -> new CabApplicationResponseDTO(
                        cab.getCabApplicationId(),
                        cab.getUsername(),
                        cab.getStatus(),
                        cab.getCreatedDate()
                )).toList();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse> updateCabApplication(@RequestBody UpdateCabApplicationRequest updateCabApplicationRequest, @PathVariable Long id) {
        return ResponseEntity.ok(cabApplicationService.updateCabApplication(updateCabApplicationRequest, id));
    }

    @GetMapping("/approved/{username}")
    public List<CabApplication> getApprovedCabDetails(@PathVariable String username){
        return cabDetailsService.getApplicationByusername(username, ApplicationStatus.APPROVED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CabApplication> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(cabApplicationService.getApplicationById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CabApplication>> getAllApplications() {
        return ResponseEntity.ok(cabApplicationService.getAllApplications());
    }

    @PostMapping("/create-bulk")
    public ResponseEntity<ApiResponse> bulkCreateCabApplications(@RequestBody List<CabApplicationRequest> requests) {
        return ResponseEntity.ok(cabApplicationService.bulkCreateApplications(requests));
    }
}
