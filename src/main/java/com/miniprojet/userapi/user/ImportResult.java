package com.miniprojet.userapi.user;

import java.util.List;

/** Summary returned by POST /api/users/batch. */
public record ImportResult(int total, int imported, int notImported, List<ImportError> errors) {

    /** Why a given record (0-based position in the file) was not imported. */
    public record ImportError(int index, String username, String email, String reason) {
    }
}
