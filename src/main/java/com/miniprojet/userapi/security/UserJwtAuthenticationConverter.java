package com.miniprojet.userapi.security;

import com.miniprojet.userapi.user.AppUser;
import com.miniprojet.userapi.user.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Turns a valid JWT into an authenticated user.
 * The role is read from the database on every request (not from the token),
 * so a role change takes effect immediately and a deleted user is rejected.
 */
@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public UserJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getSubject() == null ? "" : jwt.getSubject().toLowerCase(Locale.ROOT);
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidBearerTokenException("The user of this token does not exist"));
        return new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(user.getRole().authority())),
                user.getEmail());
    }
}
