package EmployeeDetails_Service.EmployeeDetails_Service.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "traffic-operation-service", path = "/api/assignments")
public interface TrafficServiceClient {

    @GetMapping
    List<AssignmentResponse> getAllAssignments();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AssignmentResponse {
        private Long assignmentId;
        private LocalDate assignmentDate;
        private Long driverId;
        private Long depotId;
    }
}
