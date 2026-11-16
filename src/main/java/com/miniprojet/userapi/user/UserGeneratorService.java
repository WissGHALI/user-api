package com.miniprojet.userapi.user;

import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Generates realistic users with Datafaker.
 * <p>
 * For each user a locale (country) is picked first, then the name, city and phone number are generated
 * from that same locale, so a user from FR gets a French name, a French city and a French mobile number.
 * Usernames and emails are unique inside one generated file.
 */
@Service
public class UserGeneratorService {

    static final int MIN_AGE = 18;
    static final int MAX_AGE = 70;
    static final int MIN_PASSWORD_LENGTH = 6;
    static final int MAX_PASSWORD_LENGTH = 10;

    /** Locales supported by Datafaker; the region gives the ISO 3166-1 alpha-2 country code. */
    private static final List<Locale> LOCALES = List.of(
            Locale.forLanguageTag("fr-FR"),
            Locale.forLanguageTag("en-US"),
            Locale.forLanguageTag("en-GB"),
            Locale.forLanguageTag("de-DE"),
            Locale.forLanguageTag("es-ES"),
            Locale.forLanguageTag("it-IT"),
            Locale.forLanguageTag("nl-NL"),
            Locale.forLanguageTag("en-CA"),
            Locale.forLanguageTag("en-AU"),
            Locale.forLanguageTag("pt-BR"));

    /** Real consumer mail providers, never example.com/test.com. */
    private static final List<String> MAIL_DOMAINS = List.of(
            "gmail.com", "outlook.com", "hotmail.com", "yahoo.com", "icloud.com", "proton.me", "gmx.com");

    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%&*?";
    private static final String ALL = LOWER + UPPER + DIGITS + SPECIAL;

    private final int maxCount;
    private final SecureRandom random = new SecureRandom();

    public UserGeneratorService(@Value("${app.generator.max-count:10000}") int maxCount) {
        this.maxCount = maxCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public List<UserRecord> generate(int count) {
        if (count < 1 || count > maxCount) {
            throw new IllegalArgumentException("count must be between 1 and " + maxCount);
        }
        // Faker instances are created per call: they are not guaranteed to be thread-safe.
        Map<Locale, Faker> fakers = new HashMap<>();
        Set<String> usernames = new HashSet<>();
        Set<String> emails = new HashSet<>();
        List<UserRecord> users = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Locale locale = LOCALES.get(random.nextInt(LOCALES.size()));
            Faker faker = fakers.computeIfAbsent(locale, Faker::new);

            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String base = truncate(slug(firstName) + "." + slug(lastName), 40);

            users.add(new UserRecord(
                    firstName,
                    lastName,
                    randomBirthDate(),
                    city(faker),
                    locale.getCountry(),
                    "https://i.pravatar.cc/300?img=" + (1 + random.nextInt(70)),
                    faker.company().name(),
                    faker.job().title(),
                    faker.phoneNumber().cellPhone(),
                    unique(base, usernames, ""),
                    unique(base, emails, "@" + MAIL_DOMAINS.get(random.nextInt(MAIL_DOMAINS.size()))),
                    randomPassword(),
                    random.nextBoolean() ? Role.ADMIN.getValue() : Role.USER.getValue()));
        }
        return users;
    }

    private String city(Faker faker) {
        String city = faker.address().cityName();
        return (city == null || city.isBlank()) ? faker.address().city() : city;
    }

    private LocalDate randomBirthDate() {
        LocalDate today = LocalDate.now();
        LocalDate oldest = today.minusYears(MAX_AGE).plusDays(1);
        LocalDate youngest = today.minusYears(MIN_AGE);
        long span = ChronoUnit.DAYS.between(oldest, youngest);
        return oldest.plusDays(random.nextLong(span + 1));
    }

    /** 6 to 10 characters, with at least one lower-case letter, one upper-case letter and one digit. */
    String randomPassword() {
        int length = MIN_PASSWORD_LENGTH + random.nextInt(MAX_PASSWORD_LENGTH - MIN_PASSWORD_LENGTH + 1);
        List<Character> chars = new ArrayList<>(length);
        chars.add(pick(LOWER));
        chars.add(pick(UPPER));
        chars.add(pick(DIGITS));
        while (chars.size() < length) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, random);
        StringBuilder sb = new StringBuilder(length);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }

    /** "jean.dupont" -> "jean.dupont", "jean.dupont42", ... until it is not already used. */
    private String unique(String base, Set<String> used, String suffix) {
        String candidate = random.nextInt(3) == 0 ? base + (1 + random.nextInt(99)) : base;
        while (!used.add(candidate + suffix)) {
            candidate = base + (1 + random.nextInt(9999));
        }
        return candidate + suffix;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Lower-case ASCII only: "Hélène" -> "helene", "O'Connor" -> "oconnor". */
    static String slug(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return slug.isEmpty() ? "member" : slug;
    }
}
