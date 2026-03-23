package EmployeeDetails_Service.EmployeeDetails_Service.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "aadhaar_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aadhaar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String aadhaarNumber;

    private LocalDate dateOfBirth;

    private String zip;

    private String fullName;

    private String gender;

    @Embedded
    private AadhaarAddress address;

    @Column(unique = true)
    private String clientId;

    @Lob
    private String profileImage;

    @Lob
    private String zipData;

    @Lob
    private String rawXml;

    private String shareCode;

    private String careOf;

    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;
}
