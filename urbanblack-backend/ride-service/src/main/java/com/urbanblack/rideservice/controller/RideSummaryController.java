package com.urbanblack.rideservice.controller;

import com.urbanblack.rideservice.dto.MonthlyRideSummary;
import com.urbanblack.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ride-summary")
@RequiredArgsConstructor
public class RideSummaryController {

    private final RideRepository rideRepository;

    @GetMapping("/monthly")
    public List<MonthlyRideSummary> getMonthlySummary(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(LocalTime.MAX);
        
        return rideRepository.getMonthlySummary(from, to);
    }
}
