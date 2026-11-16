package com.miniprojet.userapi.security;

import com.miniprojet.userapi.user.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;
    private final String issuer;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.jwt.expiration:1h}") Duration expiration,
                      @Value("${app.jwt.issuer:user-api}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
        this.issuer = issuer;
    }

    /** HS256 token. Subject and "email" claim = the user's email, as required. */
    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .subject(user.getEmail())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
