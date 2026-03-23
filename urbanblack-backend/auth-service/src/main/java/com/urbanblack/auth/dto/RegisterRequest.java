package com.urbanblack.auth.dto;

import com.urbanblack.auth.entity.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String mobile;
    private String password;
    private Role role;
}
