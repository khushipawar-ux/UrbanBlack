package com.traffic.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepotManagerDetailsDTO {
    private Long depotId;
    private String depotName;

    private Long managerId;
    private String managerName;
    private String managerEmail;

    private LocalDate registrationDate;
    private String status;
}
