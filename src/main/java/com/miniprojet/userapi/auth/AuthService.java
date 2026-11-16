package com.miniprojet.userapi.auth;

import com.miniprojet.userapi.security.JwtService;
import com.miniprojet.userapi.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * @param login username or email (case-insensitive)
     * @return a signed JWT whose subject (and "email" claim) is the user's email
     */
    @Transactional(readOnly = true)
    public String authenticate(String login, String password) {
        String key = login.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUsernameOrEmail(key, key)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(jwtService::generateToken)
                // Same message whether the user exists or not.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid username/email or password"));
    }
}
