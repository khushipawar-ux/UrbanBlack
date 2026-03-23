package com.urbanblack.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectionEmailEvent {
    private String candidateEmail;
    private String candidateName;
}
