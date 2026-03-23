package com.urban.cabregisterationservice.config;

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
                        .title("Urban Black – Cab Registration API")
                        .version("1.0.0")
                        .description("Fleet Management & Vehicle Compliance Service for Urban Black\n\n" +
                                "### Capabilities:\n" +
                                "- 🏎️ **Vehicle Profiling** — Detailed specifications for every cab in the network\n" +
                                "- 📑 **Compliance** — Tracking insurance, pollution certificates, and fitness logs\n" +
                                "- 🛠️ **Maintenance** — Scheduling and logging periodic vehicle health checks\n" +
                                "- 🚦 **Status Monitoring** — Real-time reporting on vehicle operational status\n\n" +
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
