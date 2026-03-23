package com.urbanblack.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpEvent {
    private String email;
    private String otp;
}
