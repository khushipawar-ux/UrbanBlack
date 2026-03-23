package com.urbanblack.paymentservice.client;

import com.urbanblack.paymentservice.dto.RidePaymentUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rideClient", url = "${services.ride.base-url}")
public interface RideClient {

    @PatchMapping("/api/v1/rides/{rideId}/payment")
    Object updateRidePayment(@PathVariable("rideId") String rideId,
                             @RequestBody RidePaymentUpdateRequest request);
}

