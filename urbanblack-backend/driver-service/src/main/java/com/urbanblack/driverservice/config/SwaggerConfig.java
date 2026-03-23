package com.urbanblack.driverservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI driverServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UrbanBlack – Driver Service API")
                        .description(
                                "REST API for driver login, shift management (clock-in/out, online/offline) " +
                                        "and driver profile.\n\n" +
                                        "**Login first** via `POST /api/driver/auth/login` to get a JWT token, " +
                                        "then click **Authorize** and paste the token.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("UrbanBlack")
                                .email("support@urbanblack.in")))
                // Add global JWT Bearer security requirement to all endpoints
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token from /api/driver/auth/login")));
    }
}
