package EmployeeDetails_Service.EmployeeDetails_Service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notifications/employee-credentials")
    void sendEmployeeCredentials(@org.springframework.web.bind.annotation.RequestBody EmployeeDetails_Service.EmployeeDetails_Service.dto.CredentialEmailRequest request);
}
