package com.urbanblack.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferLetterRequest {
    private String candidateEmail;
    private String candidateName;
    private String role;
    private Double salary;
}
