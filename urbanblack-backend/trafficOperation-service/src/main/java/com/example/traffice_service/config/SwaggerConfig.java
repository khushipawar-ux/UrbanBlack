package com.example.traffice_service.config;

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
                        .title("Urban Black – Traffic Operations API")
                        .version("1.0.0")
                        .description("Urban Logistics, Depot & Shift Management for Urban Black\n\n" +
                                "### Capabilities:\n" +
                                "- 📍 **Depot Management** — Managing physical operation centers and vehicle docking\n" +
                                "- 📅 **Shift Scheduling** — Advanced rostering for drivers and fleet movements\n" +
                                "- ⚖️ **T1-T4 Rules** — Dynamic business rule engine for urban traffic compliance\n" +
                                "- 📊 **Operational Reports** — Real-time analytics on fleet utilization and performance\n\n" +
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
