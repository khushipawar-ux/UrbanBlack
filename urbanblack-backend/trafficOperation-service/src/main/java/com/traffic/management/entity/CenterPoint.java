package com.traffic.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "center_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CenterPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pointName;
    private Double latitude;
    private Double longitude;

    @ManyToOne
    @JoinColumn(name = "depot_id")
    @JsonIgnore
    private Depot depot;

    @Column(name = "depot_id", insertable = false, updatable = false)
    private Long depotId;
}
