package com.miniprojet.userapi;

import com.jayway.jsonpath.JsonPath;
import com.miniprojet.userapi.user.AppUser;
import com.miniprojet.userapi.user.UserRecord;
import com.miniprojet.userapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end tests of the 5 endpoints through the real security filter chain. */
@SpringBootTest
class UserApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        userRepository.deleteAll();
    }

    // ---------- 1. GET /api/users/generate ----------

    @Test
    void generateTriggersAJsonFileDownload() throws Exception {
        mvc.perform(get("/api/users/generate").param("count", "5"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("users.json")))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].firstName").isNotEmpty())
                .andExpect(jsonPath("$[0].birthDate").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
                .andExpect(jsonPath("$[0].password").isNotEmpty())
                .andExpect(jsonPath("$[0].role").value(org.hamcrest.Matchers.oneOf("admin", "user")));
    }

    @Test
    void generateRejectsInvalidCount() throws Exception {
        mvc.perform(get("/api/users/generate").param("count", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/generate").param("count", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/generate")).andExpect(status().isBadRequest());
    }

    @Test
    void generatedFileCanBeImported() throws Exception {
        byte[] file = mvc.perform(get("/api/users/generate").param("count", "20"))
                .andReturn().getResponse().getContentAsByteArray();

        mvc.perform(multipart("/api/users/batch").file(jsonFile(file)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(20))
                .andExpect(jsonPath("$.imported").value(20))
                .andExpect(jsonPath("$.notImported").value(0));
    }

    // ---------- 2. POST /api/users/batch ----------

    @Test
    void importHashesPasswords() throws Exception {
        upload(user("alice.martin", "alice@gmail.com", "Secret12", "user"));

        AppUser saved = userRepository.findByUsername("alice.martin").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("Secret12").startsWith("$2");
        assertThat(passwordEncoder.matches("Secret12", saved.getPassword())).isTrue();
    }

    @Test
    void importSkipsDuplicatesInsideTheFile() throws Exception {
        mvc.perform(multipart("/api/users/batch").file(jsonFile(objectMapper.writeValueAsBytes(List.of(
                        user("alice.martin", "alice@gmail.com", "Secret12", "user"),
                        user("alice.martin", "other@gmail.com", "Secret12", "user"),   // same username
                        user("bob.durand", "ALICE@gmail.com", "Secret12", "user"),     // same email (case)
                        user("carl.petit", "carl@gmail.com", "Secret12", "admin"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.notImported").value(2))
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    @Test
    void importSkipsUsersAlreadyInDatabase() throws Exception {
        upload(user("alice.martin", "alice@gmail.com", "Secret12", "user"));

        mvc.perform(multipart("/api/users/batch").file(jsonFile(objectMapper.writeValueAsBytes(List.of(
                        user("alice.martin", "new@gmail.com", "Secret12", "user"),
                        user("someone.else", "alice@gmail.com", "Secret12", "user"))))))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.notImported").value(2));
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void importRejectsInvalidRecordsButKeepsValidOnes() throws Exception {
        mvc.perform(multipart("/api/users/batch").file(jsonFile(objectMapper.writeValueAsBytes(List.of(
                        user("good.user", "good@gmail.com", "Secret12", "user"),
                        user("bad.role", "badrole@gmail.com", "Secret12", "superadmin"),
                        user("bad.password", "badpwd@gmail.com", "123", "user"))))))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.notImported").value(2));
    }

    @Test
    void importRejectsAFileThatIsNotAJsonArray() throws Exception {
        mvc.perform(multipart("/api/users/batch").file(jsonFile("not json".getBytes())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/users/batch").file(jsonFile("{\"a\":1}".getBytes())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/users/batch").file(jsonFile(new byte[0])))
                .andExpect(status().isBadRequest());
    }

    // ---------- 3. POST /api/auth ----------

    @Test
    void loginWithUsernameOrEmailReturnsAJwtContainingTheEmail() throws Exception {
        upload(user("alice.martin", "alice@gmail.com", "Secret12", "user"));

        String byUsername = login("alice.martin", "Secret12");
        String byEmail = login("alice@gmail.com", "Secret12");

        for (String token : List.of(byUsername, byEmail)) {
            Jwt jwt = jwtDecoder.decode(token);
            assertThat(jwt.getClaimAsString("email")).isEqualTo("alice@gmail.com");
            assertThat(jwt.getSubject()).isEqualTo("alice@gmail.com");
        }
    }

    @Test
    void loginWithWrongCredentialsReturns401() throws Exception {
        upload(user("alice.martin", "alice@gmail.com", "Secret12", "user"));

        mvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice.martin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"Secret12\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 4. GET /api/users/me ----------

    @Test
    void meRequiresAToken() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheProfileWithoutPassword() throws Exception {
        upload(user("alice.martin", "alice@gmail.com", "Secret12", "user"));
        String token = login("alice.martin", "Secret12");

        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice.martin"))
                .andExpect(jsonPath("$.email").value("alice@gmail.com"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ---------- 5. GET /api/users/{username} ----------

    @Test
    void userCanSeeOwnProfileButNotOthers() throws Exception {
        upload(user("alice.martin", "alice@gmail.com", "Secret12", "user"),
                user("bob.durand", "bob@gmail.com", "Secret12", "user"));
        String token = login("alice.martin", "Secret12");

        mvc.perform(get("/api/users/alice.martin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice.martin"));
        mvc.perform(get("/api/users/bob.durand").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users/does.not.exist").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSeeAnyProfile() throws Exception {
        upload(user("admin.root", "admin@gmail.com", "Secret12", "admin"),
                user("bob.durand", "bob@gmail.com", "Secret12", "user"));
        String token = login("admin@gmail.com", "Secret12");

        mvc.perform(get("/api/users/bob.durand").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@gmail.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/users/does.not.exist").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void profileByUsernameRequiresAToken() throws Exception {
        mvc.perform(get("/api/users/bob.durand")).andExpect(status().isUnauthorized());
    }

    // ---------- Swagger ----------

    @Test
    void openApiDocumentIsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    // ---------- helpers ----------

    private static UserRecord user(String username, String email, String password, String role) {
        return new UserRecord("Alice", "Martin", LocalDate.of(1990, 4, 12), "Lyon", "FR",
                "https://i.pravatar.cc/300?img=5", "Dupont SARL", "Software Engineer", "06 12 34 56 78",
                username, email, password, role);
    }

    private void upload(UserRecord... users) throws Exception {
        mvc.perform(multipart("/api/users/batch").file(jsonFile(objectMapper.writeValueAsBytes(List.of(users)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(users.length));
    }

    private static MockMultipartFile jsonFile(byte[] content) {
        return new MockMultipartFile("file", "users.json", MediaType.APPLICATION_JSON_VALUE, content);
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
