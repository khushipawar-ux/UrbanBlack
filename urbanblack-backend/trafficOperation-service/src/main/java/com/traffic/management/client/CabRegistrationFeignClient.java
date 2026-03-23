package com.traffic.management.client;

import com.traffic.management.dto.VehicleResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "cab-registration-service", path = "/registration")
public interface CabRegistrationFeignClient {

    @GetMapping("/all")
    List<VehicleResponseDTO> getAllVehicles();

    @GetMapping("/{id}")
    VehicleResponseDTO getVehicleById(@PathVariable("id") Long id);

    @PatchMapping("/{id}")
    void updateVehicleStatus(@PathVariable("id") Long id, 
                             @RequestBody com.traffic.management.dto.UpdateStatusRequest updateRequest);
}
