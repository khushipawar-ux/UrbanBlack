package com.urban.cabregisterationservice.controller;

import com.urban.cabregisterationservice.dto.RcDetailsRequest;
import com.urban.cabregisterationservice.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/rc-details")
    public ResponseEntity<Map> getRcDetails(@RequestBody RcDetailsRequest rcDetailsRequest) {
        return ResponseEntity.ok(verificationService.verifyAndSaveLicense(rcDetailsRequest));
    }

}
//