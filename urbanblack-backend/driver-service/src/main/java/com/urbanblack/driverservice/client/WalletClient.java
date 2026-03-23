package com.urbanblack.driverservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "wallet-service", url = "http://localhost:8085")
public interface WalletClient {

    @PostMapping("/api/wallet/shift-payment")
    void processShiftPayment(
            @RequestParam String driverId,
            @RequestParam long totalSeconds
    );
}
