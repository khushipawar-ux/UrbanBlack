package com.urbanblack.rideservice.dto.request;

import lombok.Data;

@Data
public class DriverLocationUpdateRequest {
    private double lat;
    private double lng;
    private double bearing;
    private double speedKmh;
    /** Driver's cab category: economy or premium. Used to filter ride offers by vehicle type. */
    private String vehicleType;
}

