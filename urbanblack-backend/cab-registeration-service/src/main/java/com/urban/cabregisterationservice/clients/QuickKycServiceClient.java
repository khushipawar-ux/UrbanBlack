package com.urban.cabregisterationservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "quickKyc-service")
public interface QuickKycServiceClient {

    @PostMapping("/internal/api/quickkyc/rc-advance")
    Map<String, Object> rcDetails(@RequestBody Map<String, String> request);
}
//