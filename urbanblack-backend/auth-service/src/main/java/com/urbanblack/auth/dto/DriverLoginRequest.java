package com.urbanblack.auth.dto;

import lombok.Data;

@Data
public class DriverLoginRequest {
    private String username;
    private String password;
}
