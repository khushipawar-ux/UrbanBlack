package com.urbanblack.rideservice.dto.request;

import lombok.Data;

@Data
public class RideRequestDto {
    private double pickupLat;
    private double pickupLng;
    private double dropLat;
    private double dropLng;
    private String pickupAddress;
    private String dropAddress;
    private String notes;
    /** Vehicle type: economy or premium. Used to match drivers with same cab category. */
    private String vehicleType;
}

