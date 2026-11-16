package com.miniprojet.userapi.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * One element of the JSON file: produced by GET /api/users/generate and read by POST /api/users/batch.
 * The password here is in clear text (it is the file the user logs in with); it is hashed before storage.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"firstName", "lastName", "birthDate", "city", "country", "avatar", "company",
        "jobPosition", "mobile", "username", "email", "password", "role"})
public record UserRecord(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull @Past @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate birthDate,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be an ISO 3166-1 alpha-2 code") String country,
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+", message = "must be an http(s) URL") String avatar,
        @NotBlank @Size(max = 150) String company,
        @NotBlank @Size(max = 150) String jobPosition,
        @NotBlank @Size(max = 50) String mobile,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                message = "must be 3-50 characters: letters, digits, '.', '_' or '-'") String username,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 6, max = 10) String password,
        @NotBlank @Pattern(regexp = "^(?i)(admin|user)$", message = "must be 'admin' or 'user'") String role) {
}
