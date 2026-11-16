package com.miniprojet.userapi.user;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/** Public view of a user: everything except the password hash. */
public record UserProfile(
        String firstName,
        String lastName,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate birthDate,
        String city,
        String country,
        String avatar,
        String company,
        String jobPosition,
        String mobile,
        String username,
        String email,
        String role) {

    public static UserProfile from(AppUser user) {
        return new UserProfile(
                user.getFirstName(),
                user.getLastName(),
                user.getBirthDate(),
                user.getCity(),
                user.getCountry(),
                user.getAvatar(),
                user.getCompany(),
                user.getJobPosition(),
                user.getMobile(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getValue());
    }
}
