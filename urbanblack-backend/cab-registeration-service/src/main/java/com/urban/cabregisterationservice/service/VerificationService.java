package com.urban.cabregisterationservice.service;

import com.urban.cabregisterationservice.clients.QuickKycServiceClient;
import com.urban.cabregisterationservice.dto.RcDetailsRequest;
import com.urban.cabregisterationservice.entity.RcDetails;
import com.urbanblack.common.enums.CabModel;
import com.urban.cabregisterationservice.repository.RcDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
//
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final QuickKycServiceClient quickKycServiceClient;
    private final RcDetailsRepository rcDetailsRepository;

    public Map<String, Object> verifyAndSaveLicense(RcDetailsRequest rcDetailsRequest) {
        Map<String, Object> response;
        log.info("Processing RC Verification for: {}", rcDetailsRequest);

        if (rcDetailsRequest.getRcNumber() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rcNumber cannot be null");
        }

        try {
            // 1. Prepare payload for Proxy Service
            Map<String, String> proxyRequest = new HashMap<>();
            proxyRequest.put("id_number", rcDetailsRequest.getRcNumber());
            proxyRequest.put("chassis_number", rcDetailsRequest.getChassisNumber());
            proxyRequest.put("engine_number", rcDetailsRequest.getEngineNumber());

            // 2. Call Proxy Service (mock or real)
            response = quickKycServiceClient.rcDetails(proxyRequest);

            // 3. Parse response and save to database
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");

                if (data != null) {
                    RcDetails rcDetails = new RcDetails();
                    rcDetails.setRcNumber(getStringValue(data, "rc_number"));
                    rcDetails.setOwnerName(getStringValue(data, "owner_name"));
                    String modelStr = getStringValue(data, "vehicle_model");
                    if (modelStr != null) {
                        try {
                            rcDetails.setVehicleModel(CabModel.valueOf(modelStr.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid vehicle model from KYC: {}. Setting to null.", modelStr);
                        }
                    }
                    rcDetails.setFuelType(getStringValue(data, "fuel_type"));
                    rcDetails.setRegistrationDate(getStringValue(data, "registration_date"));
                    rcDetails.setFitUpToDate(getStringValue(data, "fit_up_to"));
                    rcDetails.setInsuranceCompanyName(getStringValue(data, "insurance_company"));
                    rcDetails.setInsurancePolicyNumber(getStringValue(data, "insurance_policy_number"));
                    rcDetails.setInsuranceUptoDate(getStringValue(data, "insurance_upto"));
                    rcDetails.setVehicleChasiNumber(getStringValue(data, "vehicle_chasi_number"));
                    rcDetails.setVehicleEngineNumber(getStringValue(data, "vehicle_engine_number"));
                    rcDetails.setRcStatus(getStringValue(data, "rc_status"));
                    rcDetails.setChallanDetails(getStringValue(data, "challan_details"));
                    rcDetails.setOtherDetails(getStringValue(data, "other_details"));

                    RcDetails savedRcDetails = rcDetailsRepository.save(rcDetails);
                    log.info("RC Details saved to database with ID: {}", savedRcDetails.getId());

                    // Add the saved ID to the response
                    response.put("savedRcDetailsId", savedRcDetails.getId());
                } else {
                    log.warn("RC verification response has no 'data' field");
                }
            } else {
                log.warn("RC verification failed or returned unsuccessful response");
            }

        } catch (Exception e) {
            log.error("Error in RC verification", e);
            throw new RuntimeException("Error in RC Api", e);
        }

        return response;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
