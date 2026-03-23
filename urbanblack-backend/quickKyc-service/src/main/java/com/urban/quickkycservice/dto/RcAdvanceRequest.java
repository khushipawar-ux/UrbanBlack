package com.urban.quickkycservice.dto;

import lombok.Data;

@Data
public class RcAdvanceRequest {
    private String id_number;
    private String chassis_number;
    private String engine_number;
    private String owner_name;
    private String consent;
}
