package EmployeeDetails_Service.EmployeeDetails_Service.Entity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "driving_license")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrivingLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    private String licenseNumber;
    private String name;
    private String permanentAddress;
    private String permanentZip;
    private String licenseType;
    private String fatherOrHusbandName;
    private LocalDate licenseExpiryDate;
    private LocalDate transportIssueDate;
    private LocalDate transportExpiryDate;
    private String vehicleClasses;

    @Column(columnDefinition = "TEXT")
    private String otherDetails;

    @OneToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;
}

