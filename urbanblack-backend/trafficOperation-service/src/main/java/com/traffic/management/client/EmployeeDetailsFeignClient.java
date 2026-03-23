package com.traffic.management.client;

import com.urbanblack.common.dto.employee.EmployeeResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "employee-details-service", path = "/employees")
public interface EmployeeDetailsFeignClient {

    @GetMapping
    List<EmployeeResponseDTO> getAllEmployees();

    @GetMapping("/role/{role}")
    List<EmployeeResponseDTO> getEmployeesByRole(@PathVariable("role") String role);

    @GetMapping("/{id}")
    EmployeeResponseDTO getEmployeeById(@PathVariable("id") Long id);
}
