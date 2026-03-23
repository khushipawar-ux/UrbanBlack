package com.urbanblack.driverservice.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftCompletedEvent {
    private String shiftId;
    private String driverId;
    private long totalWorkedSeconds;
}