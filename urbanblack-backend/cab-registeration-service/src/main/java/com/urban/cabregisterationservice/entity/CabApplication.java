package com.urban.cabregisterationservice.entity;

import com.urbanblack.common.enums.ApplicationStage;
import com.urbanblack.common.enums.ApplicationStatus;
import com.urbanblack.common.enums.CabCategory;
import com.urbanblack.common.enums.CabModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "cab_application")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CabApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cabApplicationId;

    private String username;

    private String numberPlate;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "rc_number_id")
    private RcDetails rcNumber;

    private String carName;

    @Enumerated(EnumType.STRING)
    private CabModel cabModel;

    @Enumerated(EnumType.STRING)
    private CabCategory category;

    private String acType; // AC or NON-AC

    private Integer vehicleYear;

    private Long kms;

    private String passingDate;

    private String fuelType;

    private BigDecimal packageAmount;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    private ApplicationStage stage;

    @CreationTimestamp
    private LocalDateTime createdDate;

}
