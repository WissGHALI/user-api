package com.miniprojet.userapi.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Imports the JSON file produced by the generator.
 * <p>
 * Every record is checked on its own (format, validation, duplicates inside the file and in the database)
 * before anything is saved, so one bad record never rolls back the others.
 */
@Service
public class UserImportService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public UserImportService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                             ObjectMapper objectMapper, Validator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional
    public ImportResult importFile(MultipartFile file) {
        JsonNode root = readArray(file);

        Set<String> usernamesInFile = new HashSet<>();
        Set<String> emailsInFile = new HashSet<>();
        List<AppUser> toSave = new ArrayList<>();
        List<ImportResult.ImportError> errors = new ArrayList<>();

        for (int i = 0; i < root.size(); i++) {
            UserRecord record;
            try {
                record = objectMapper.treeToValue(root.get(i), UserRecord.class);
            } catch (JacksonException e) {
                errors.add(new ImportResult.ImportError(i, null, null,
                        "Malformed record (wrong field type or date not in yyyy-MM-dd format)"));
                continue;
            }
            if (record == null) {
                errors.add(new ImportResult.ImportError(i, null, null, "Empty record"));
                continue;
            }

            String username = normalize(record.username());
            String email = normalize(record.email());

            String reason = validate(record);
            if (reason == null) {
                if (usernamesInFile.contains(username)) {
                    reason = "Duplicate username in the file";
                } else if (emailsInFile.contains(email)) {
                    reason = "Duplicate email in the file";
                } else if (userRepository.existsByUsername(username)) {
                    reason = "Username already exists";
                } else if (userRepository.existsByEmail(email)) {
                    reason = "Email already exists";
                }
            }
            if (reason != null) {
                errors.add(new ImportResult.ImportError(i, record.username(), record.email(), reason));
                continue;
            }

            usernamesInFile.add(username);
            emailsInFile.add(email);
            toSave.add(toEntity(record, username, email));
        }

        userRepository.saveAll(toSave);
        int total = root.size();
        return new ImportResult(total, toSave.size(), total - toSave.size(), errors);
    }

    private JsonNode readArray(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is missing or empty");
        }
        JsonNode root;
        try (InputStream in = file.getInputStream()) {
            root = objectMapper.readTree(in);
        } catch (IOException | JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is not valid JSON");
        }
        if (root == null || !root.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file must contain a JSON array of users");
        }
        return root;
    }

    /** Returns null when valid, otherwise a readable list of problems. */
    private String validate(UserRecord record) {
        Set<ConstraintViolation<UserRecord>> violations = validator.validate(record);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; ", "Invalid record: ", ""));
    }

    private AppUser toEntity(UserRecord record, String username, String email) {
        AppUser user = new AppUser();
        user.setFirstName(record.firstName().trim());
        user.setLastName(record.lastName().trim());
        user.setBirthDate(record.birthDate());
        user.setCity(record.city().trim());
        user.setCountry(record.country().trim().toUpperCase(Locale.ROOT));
        user.setAvatar(record.avatar().trim());
        user.setCompany(record.company().trim());
        user.setJobPosition(record.jobPosition().trim());
        user.setMobile(record.mobile().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(record.password()));
        user.setRole(Role.fromValue(record.role()).orElseThrow());
        return user;
    }

    static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
