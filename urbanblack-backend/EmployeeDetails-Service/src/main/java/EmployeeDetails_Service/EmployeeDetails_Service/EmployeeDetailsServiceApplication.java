package EmployeeDetails_Service.EmployeeDetails_Service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
public class EmployeeDetailsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmployeeDetailsServiceApplication.class, args);
	}

}
