package com.miniprojet.userapi.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "3. Log in and get a JWT",
            description = "The username field accepts either the username or the email from the imported file. "
                    + "The returned token contains the user's email.")
    @ApiResponse(responseCode = "200", description = "Token issued")
    @ApiResponse(responseCode = "400", description = "username or password missing")
    @ApiResponse(responseCode = "401", description = "Wrong credentials")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse authenticate(@Valid @RequestBody AuthRequest request) {
        return new AuthResponse(authService.authenticate(request.username(), request.password()));
    }

    public record AuthRequest(
            @Schema(description = "Username or email", example = "jean.dupont") @NotBlank String username,
            @Schema(description = "Clear password from the generated file") @NotBlank String password) {
    }

    public record AuthResponse(String accessToken) {
    }
}
