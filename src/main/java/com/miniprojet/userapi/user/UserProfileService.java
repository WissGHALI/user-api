package com.miniprojet.userapi.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The JWT subject is the user's email. */
    public UserProfile me(String email) {
        return UserProfile.from(currentUser(email));
    }

    /**
     * A user can always see their own profile. Only an admin can see someone else's.
     * A non-admin gets 403 whether the other user exists or not, so usernames cannot be probed.
     */
    public UserProfile byUsername(String requesterEmail, String username) {
        AppUser requester = currentUser(requesterEmail);
        String wanted = UserImportService.normalize(username);
        if (requester.getUsername().equals(wanted)) {
            return UserProfile.from(requester);
        }
        if (requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an admin can view another user's profile");
        }
        return userRepository.findByUsername(wanted)
                .map(UserProfile::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
    }

    private AppUser currentUser(String email) {
        return userRepository.findByEmail(UserImportService.normalize(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "The user of this token no longer exists"));
    }
}
