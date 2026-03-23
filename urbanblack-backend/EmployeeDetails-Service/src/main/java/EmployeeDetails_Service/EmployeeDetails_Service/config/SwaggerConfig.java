package EmployeeDetails_Service.EmployeeDetails_Service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI employeeDetailsOpenAPI() {

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8086");
        localServer.setDescription("Employee Details Service (Direct Host Port)");

        Server gatewayServer = new Server();
        gatewayServer.setUrl("http://localhost:8080");
        gatewayServer.setDescription("API Gateway (Consolidated)");

        Contact contact = new Contact()
                .email("support@urbanblack.com")
                .name("Urban Black Support")
                .url("https://urbanblack.com");

        License license = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Urban Black – Employee Service API")
                .version("1.0.0")
                .contact(contact)
                .description("Internal Staff Records & Operational Controls for Urban Black\n\n" +
                        "### Capabilities:\n" +
                        "- 👔 **Staff Records** — Central repository for administrative and operational personnel\n" +
                        "- 🏢 **Hierarchy** — Management of departments and reporting structures\n" +
                        "- 🗝️ **OTP Auth** — Secure one-time password module for internal dashboard access\n" +
                        "- 📩 **Onboarding** — Automated credential generation and email dispatch\n\n" +
                        "**Dev Note:** OTPs are currently printed to the backend console for verification.")
                .termsOfService("https://urbanblack.com/terms")
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, gatewayServer))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createJwtSecurityScheme()));
    }

    private SecurityScheme createJwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your JWT token from the /auth/login response (without 'Bearer ' prefix)");
    }
}
