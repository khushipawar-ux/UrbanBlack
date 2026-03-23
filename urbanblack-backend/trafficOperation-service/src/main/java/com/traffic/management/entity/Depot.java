package com.traffic.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "depots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String depotCode;

    private String depotName;
    private String city;
    private String fullAddress;

    private Double latitude;
    private Double longitude;

    private String zone;
    private Integer capacity;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime operatingStart;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime operatingEnd;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate registrationDate;

    @Builder.Default
    private Boolean isActive = true;

    // Relationship Mapping
    @OneToMany(mappedBy = "depot", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<CenterPoint> centerPoints;
}
