package com.urbanblack.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectionEmailEvent {
    private String candidateEmail;
    private String candidateName;
    private String role;
    private String slotDate;
    private String slotTime;
    private String interviewAddress;
}
