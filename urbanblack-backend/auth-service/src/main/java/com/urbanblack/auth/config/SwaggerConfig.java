package com.urbanblack.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Urban Black – Auth Service API")
                        .version("1.0.0")
                        .description("Centralized Authentication & Authorization Service for Urban Black\n\n" +
                                "### Capabilities:\n" +
                                "- 🔐 **JWT Management** — Token generation, validation, and refresh\n" +
                                "- 🛡️ **User Authentication** — Secure login, signup, and account verification\n" +
                                "- 👮 **RBAC** — Role-based access control for Admins, Drivers, and Employees\n" +
                                "- 🔑 **Security** — Integration with Shared Secret for cluster-wide verification\n\n" +
                                "Contact: [Urban Black Support](mailto:support@urbanblack.com)"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}
