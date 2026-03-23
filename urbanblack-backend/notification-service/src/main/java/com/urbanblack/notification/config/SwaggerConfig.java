package com.urbanblack.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI notificationOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8091");
        server.setDescription("Notification Service");

        Contact contact = new Contact()
                .email("support@urbanblack.com")
                .name("Urban Black Support");

        Info info = new Info()
                .title("Urban Black – Notification Service API")
                .version("1.0.0")
                .contact(contact)
                .description("""
                        **Centralized Notification Service for Urban Black**

                        Capabilities:
                        - 📧 **Email Notifications** — Welcome emails, credentials, OTP, alerts
                        - 📋 **Notification Logs** — Full audit trail for every notification sent
                        - ⚡ **Kafka Consumer** — Receives events from other microservices on topics:
                          - `notification.email` — Generic email payloads
                          - `notification.credentials` — Employee onboarding credentials

                        **Integration**: Other services post to `/notifications/email/send` (REST) or
                        publish to Kafka topics for fully decoupled async notification delivery.
                        """);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
