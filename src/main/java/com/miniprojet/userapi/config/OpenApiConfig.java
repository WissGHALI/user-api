package com.miniprojet.userapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the "bearerAuth" scheme so Swagger UI shows the "Authorize" button
 * and secured endpoints can be tested from the UI.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "User API",
        version = "1.0.0",
        description = "Génération, import et consultation d'utilisateurs. "
                + "Pour les endpoints sécurisés : appeler POST /api/auth, copier accessToken, "
                + "puis cliquer sur 'Authorize' et coller le jeton."))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
