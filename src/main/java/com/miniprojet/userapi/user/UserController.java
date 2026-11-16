package com.miniprojet.userapi.user;

import com.miniprojet.userapi.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserGeneratorService generatorService;
    private final UserImportService importService;
    private final UserProfileService profileService;
    private final ObjectMapper objectMapper;

    public UserController(UserGeneratorService generatorService, UserImportService importService,
                          UserProfileService profileService, ObjectMapper objectMapper) {
        this.generatorService = generatorService;
        this.importService = importService;
        this.profileService = profileService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "1. Generate users",
            description = "Returns a downloadable users.json file (Content-Disposition: attachment) "
                    + "containing `count` realistic users. In Swagger UI, use the 'Download file' link.")
    @ApiResponse(responseCode = "200", description = "users.json file",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserRecord.class)))
    @ApiResponse(responseCode = "400", description = "count missing, not a number, or out of range")
    @GetMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generate(
            @Parameter(description = "Number of users to generate (1 to 10000)", example = "100")
            @RequestParam int count) {
        List<UserRecord> users;
        try {
            users = generatorService.generate(count);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        byte[] body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(users);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("users.json").build().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(body.length)
                .body(body);
    }

    @Operation(summary = "2. Import a users file",
            description = "Uploads the JSON file produced by /api/users/generate. Users whose username or email "
                    + "already exists (in the database or earlier in the same file) are not imported. "
                    + "Passwords are hashed with BCrypt before being stored.")
    @ApiResponse(responseCode = "200", description = "Import summary")
    @ApiResponse(responseCode = "400", description = "File missing, empty, or not a JSON array")
    @ApiResponse(responseCode = "413", description = "File larger than 20MB")
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ImportResult batch(@RequestPart("file") MultipartFile file) {
        return importService.importFile(file);
    }

    @Operation(summary = "4. My profile", description = "Profile of the user who owns the JWT.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", description = "Profile")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token")
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserProfile me(Authentication authentication) {
        return profileService.me(authentication.getName());
    }

    @Operation(summary = "5. Profile by username",
            description = "An admin can view any profile. A user can only view their own.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", description = "Profile")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "403", description = "Not an admin and not your own username")
    @ApiResponse(responseCode = "404", description = "Admin only: no user with this username")
    @GetMapping(value = "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserProfile byUsername(@PathVariable String username, Authentication authentication) {
        return profileService.byUsername(authentication.getName(), username);
    }
}
