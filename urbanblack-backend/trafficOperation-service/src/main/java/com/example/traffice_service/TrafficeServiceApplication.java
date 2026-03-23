package com.example.traffice_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.traffice_service", "com.traffic.management"})
@EntityScan(basePackages = {"com.example.traffice_service", "com.traffic.management"})
@EnableJpaRepositories(basePackages = {"com.example.traffice_service", "com.traffic.management"})
@org.springframework.cloud.openfeign.EnableFeignClients(basePackages = "com.traffic.management.client")
public class TrafficeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficeServiceApplication.class, args);
	}

}
