package com.traffic.management.controller;

import com.traffic.management.client.EmployeeDetailsFeignClient;
import com.urbanblack.common.dto.employee.EmployeeResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/external/employees")
@RequiredArgsConstructor
public class ExternalEmployeeController {

    private final EmployeeDetailsFeignClient employeeDetailsFeignClient;

    @GetMapping("/role/{role}")
    public List<EmployeeResponseDTO> getByRole(@PathVariable String role) {
        return employeeDetailsFeignClient.getEmployeesByRole(role);
    }

    @GetMapping("/{id}")
    public EmployeeResponseDTO getById(@PathVariable Long id) {
        return employeeDetailsFeignClient.getEmployeeById(id);
    }
}

