package com.fitnesstracker.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fitness Tracker: User Service")
                        .version("1.0.0")
                        .description("Automated API Documentation for R&D Services"))
                // Define the categories for the single-page view
                .tags(List.of(
                        new Tag().name("1. User Facing").description("Client-side endpoints (Requires Gateway Header)"),
                        new Tag().name("2. Internal").description("Service-to-service communication"),
                        new Tag().name("3. Admin").description("Management and administrative tools")
                ));
    }

    @Bean
    public OpenApiCustomizer pathBasedTaggingCustomizer() {
        return openApi -> {
            openApi.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperations().forEach(operation -> {
                    
                    // Logic: Assign Tags and Headers based on the endpoint URL path
                    if (path.startsWith("/api/users")) {
                        operation.setTags(List.of("1. User Facing"));

                    } else if (path.startsWith("/internal")) {
                        operation.setTags(List.of("2. Internal"));

                    } else if (path.startsWith("/api/admin")) {
                        operation.setTags(List.of("3. Admin"));
                    }
                });
            });
        };
    }
}