package EmployeeDetails_Service.EmployeeDetails_Service.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "employee_package")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String designation;

    private Integer durationMonths;

    private Double inHandSalary;

    private Integer monthlyOff;

}
