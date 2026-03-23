package com.traffic.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stage_patti")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stageId;

    @Column(nullable = false)
    private Double minDistance;

    @Column(nullable = false)
    private Double maxDistance;

    @Column(nullable = false)
    private Double nonAcFare;

    @Column(nullable = false)
    private Double acPercentage; // default 10%

    private Boolean isActive = true;

    private LocalDateTime createdAt = LocalDateTime.now();
}
