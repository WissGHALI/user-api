package com.miniprojet.userapi.user;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Role {
    ADMIN("admin"),
    USER("user");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    /** Value used in the JSON files ("admin" / "user"). */
    public String getValue() {
        return value;
    }

    /** Spring Security authority, e.g. ROLE_ADMIN. */
    public String authority() {
        return "ROLE_" + name();
    }

    public static Optional<Role> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(r -> r.value.equals(normalized)).findFirst();
    }
}
