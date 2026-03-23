package com.urban.quickkycservice.config;

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
                        .title("Urban Black – Quick KYC API")
                        .version("1.0.0")
                        .description("Rapid Identity Verification & Document Processing for Urban Black\n\n" +
                                "### Capabilities:\n" +
                                "- ⚡ **Instant Verification** — Mock-integrated identity matching for 0-wait onboarding\n" +
                                "- 📂 **Document OCR** — automated parsing of licenses and national IDs (Mock)\n" +
                                "- 🤖 **Approval Engine** — Rule-based automated verification results\n" +
                                "- 🔗 **Service Integration** — Feeds verified status directly to Driver and Employee services\n\n" +
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
