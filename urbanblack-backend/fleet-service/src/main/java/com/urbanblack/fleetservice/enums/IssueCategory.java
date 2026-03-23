package com.urbanblack.fleetservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IssueCategory {
    VEHICLE("vehicle"),
    TRIP("trip"),
    VEHICLE_BREAKDOWN("vehicle_breakdown"),
    ENGINE_PROBLEM("engine_problem"),
    TIRE_ISSUE("tire_issue"),
    BRAKE_PROBLEM("brake_problem"),
    ROUTE_BLOCKED("route_blocked"),
    TRAFFIC_ACCIDENT("traffic_accident"),
    OTHER("other");

    private final String value;

    IssueCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static IssueCategory fromValue(String value) {
        if (value == null)
            return null;
        for (IssueCategory category : values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid category: " + value);
    }
}