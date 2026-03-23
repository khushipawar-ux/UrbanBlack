package com.urbanblack.rideservice.dto.request;

import lombok.Data;

@Data
public class FeedbackRequest {
    private int rating;
    private String comment;
}

