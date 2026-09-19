package com.meridiantrust.sentinel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sentinelOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sentinel AML API")
                        .version("v1")
                        .description("Real-time money laundering detection, alerting, and case management "
                                + "API for MeridianTrust's Transaction Monitoring System. "
                                + "Authenticate with HTTP Basic - see README for demo credentials."))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
