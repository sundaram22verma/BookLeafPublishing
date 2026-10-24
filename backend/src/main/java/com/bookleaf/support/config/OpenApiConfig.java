package com.bookleaf.support.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html, OpenAPI JSON at /v3/api-docs. Use "Authorize" with the login token. */
@Configuration
@OpenAPIDefinition(info = @Info(title = "BookLeaf Author Support API", version = "1.0",
        description = "Author support tickets with AI-assisted triage and response drafting"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
