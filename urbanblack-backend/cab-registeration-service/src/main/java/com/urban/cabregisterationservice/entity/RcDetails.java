package com.urban.cabregisterationservice.entity;

import com.urbanblack.common.enums.CabModel;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "RC_Details")
public class RcDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rcNumber;

    private String fitUpToDate;

    private String registrationDate;

    private String ownerName;

    private String vehicleChasiNumber;

    private String vehicleEngineNumber;

    @Enumerated(EnumType.STRING)
    private CabModel vehicleModel;

    private String fuelType;

    private String insuranceCompanyName;

    private String insurancePolicyNumber;

    private String  insuranceUptoDate;

    private String rcStatus;

    @Column(columnDefinition = "TEXT")
    private String challanDetails;

    @Column(columnDefinition = "TEXT")
    private String otherDetails;

}
//