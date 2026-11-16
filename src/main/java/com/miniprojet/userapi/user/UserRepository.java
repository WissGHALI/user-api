package com.miniprojet.userapi.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** All lookups expect lower-cased values (username and email are stored lower-cased). */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    /** Usernames cannot contain '@' (validated at import), so at most one row can match. */
    Optional<AppUser> findByUsernameOrEmail(String username, String email);
}
