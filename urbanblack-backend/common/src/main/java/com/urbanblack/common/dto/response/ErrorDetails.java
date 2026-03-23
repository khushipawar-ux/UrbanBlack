package com.urbanblack.common.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorDetails {

    private String code;
    private String message;
    private Object details;

}