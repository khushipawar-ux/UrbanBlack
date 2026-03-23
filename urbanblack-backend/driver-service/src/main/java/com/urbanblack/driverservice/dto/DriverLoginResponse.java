package com.urbanblack.driverservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLoginResponse {
    private String accessToken;
    private String driverId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String depotName;
    private String employeeId;
}