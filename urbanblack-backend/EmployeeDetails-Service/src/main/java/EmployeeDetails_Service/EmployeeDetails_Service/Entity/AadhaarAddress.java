package EmployeeDetails_Service.EmployeeDetails_Service.Entity;

import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarAddress {

    private String country;
    private String district;
    private String state;
    private String pin;
    private String postOffice;
    private String locality;
    private String vtc;
    private String subDistrict;
    private String street;
    private String house;
    private String landmark;
}

