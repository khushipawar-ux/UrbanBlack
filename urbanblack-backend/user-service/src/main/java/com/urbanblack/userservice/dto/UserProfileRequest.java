package com.urbanblack.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;
}
