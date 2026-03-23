package EmployeeDetails_Service.EmployeeDetails_Service.client;

import com.urbanblack.common.dto.request.UserRegistrationRequest;
import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.common.dto.response.SimpleMessageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "${auth-service.url:http://auth-service:8081}")
public interface AuthServiceClient {

    @PostMapping("/auth/admin/onboard-driver")
    ApiResponse<SimpleMessageResponse> onboardDriver(
            @RequestBody UserRegistrationRequest request,
            @RequestHeader("X-Role") String role
    );
}
