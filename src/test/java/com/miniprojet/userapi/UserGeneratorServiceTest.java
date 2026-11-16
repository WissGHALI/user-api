package com.miniprojet.userapi;

import com.miniprojet.userapi.user.UserGeneratorService;
import com.miniprojet.userapi.user.UserRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit test: no Spring context. */
class UserGeneratorServiceTest {

    private final UserGeneratorService generator = new UserGeneratorService(10_000);

    @Test
    void generatesTheRequestedNumberOfUsers() {
        assertThat(generator.generate(1)).hasSize(1);
        assertThat(generator.generate(250)).hasSize(250);
    }

    @Test
    void rejectsCountOutOfRange() {
        assertThatThrownBy(() -> generator.generate(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generate(-5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generate(10_001)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyFieldIsFilled() {
        for (UserRecord u : generator.generate(300)) {
            Stream.of(u.firstName(), u.lastName(), u.city(), u.country(), u.avatar(), u.company(),
                            u.jobPosition(), u.mobile(), u.username(), u.email(), u.password(), u.role())
                    .forEach(value -> assertThat(value).isNotBlank());
            assertThat(u.birthDate()).isNotNull();
        }
    }

    @Test
    void passwordIsBetween6And10Characters() {
        for (UserRecord u : generator.generate(1000)) {
            assertThat(u.password().length()).isBetween(6, 10);
        }
    }

    @Test
    void roleIsAdminOrUserAndBothAppear() {
        List<UserRecord> users = generator.generate(500);
        Set<String> roles = users.stream().map(UserRecord::role).collect(Collectors.toSet());
        assertThat(roles).containsExactlyInAnyOrder("admin", "user");
    }

    @Test
    void countryIsAnIsoAlpha2Code() {
        Set<String> isoCountries = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);
        for (UserRecord u : generator.generate(300)) {
            assertThat(u.country()).matches("[A-Z]{2}");
            assertThat(isoCountries).contains(u.country());
        }
    }

    @Test
    void birthDateGivesAnAdultAge() {
        LocalDate today = LocalDate.now();
        for (UserRecord u : generator.generate(500)) {
            int age = Period.between(u.birthDate(), today).getYears();
            assertThat(age).isBetween(18, 70);
        }
    }

    @Test
    void usernamesAndEmailsAreUniqueAndWellFormed() {
        List<UserRecord> users = generator.generate(2000);
        assertThat(users.stream().map(UserRecord::username).distinct().count()).isEqualTo(users.size());
        assertThat(users.stream().map(UserRecord::email).distinct().count()).isEqualTo(users.size());
        for (UserRecord u : users) {
            assertThat(u.username()).matches("[a-z0-9._-]{3,50}");
            assertThat(u.email()).matches("[a-z0-9.]+@[a-z0-9.-]+\\.[a-z]{2,}");
        }
    }

    @Test
    void noPlaceholderContent() {
        for (UserRecord u : generator.generate(500)) {
            assertThat(u.email()).doesNotContain("example").doesNotContain("@test.");
            assertThat(u.avatar()).startsWith("https://").doesNotContain("example");
        }
    }
}
