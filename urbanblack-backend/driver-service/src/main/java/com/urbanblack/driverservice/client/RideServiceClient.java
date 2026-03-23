package com.urbanblack.driverservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.urbanblack.driverservice.dto.MonthlyRideSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ride-service", path = "/api/v1/ride-summary")
public interface RideServiceClient {

    @GetMapping("/monthly")
    List<MonthlyRideSummaryDTO> getMonthlySummary(
            @RequestParam("month") int month,
            @RequestParam("year") int year);
}
