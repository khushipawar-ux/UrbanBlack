package com.example.traffice_service.t2.model;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "t2")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class T2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subCenterId;

    @Column(nullable = false)
    private String subCenterName;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String status;   // ACTIVE / INACTIVE

    // Depot Mapping (T1)
    @Column(nullable = false)
    private Long depotId;

    // Traffic Inspector (who created it)
    @Column(nullable = false)
    private Long inspectorId;

    private Boolean isDeleted = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}

