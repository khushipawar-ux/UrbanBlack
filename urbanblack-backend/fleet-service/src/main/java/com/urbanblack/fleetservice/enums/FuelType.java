package com.urbanblack.fleetservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FuelType {
    CNG,
    DIESEL,
    PETROL,
    ELECTRIC,
    LNG;

    @JsonCreator
    public static FuelType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FuelType type : FuelType.values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported fuelType: " + value);
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
