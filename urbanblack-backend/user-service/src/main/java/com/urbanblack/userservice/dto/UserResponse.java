package com.urbanblack.userservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;

    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}