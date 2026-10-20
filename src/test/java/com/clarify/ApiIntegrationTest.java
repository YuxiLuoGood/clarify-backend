package com.clarify;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end through HTTP, security, validation, JPA and Flyway against a
 * real PostgreSQL. Skipped automatically when Docker is not available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String signUp() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String body = mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "password123", "name", "Test"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private String createExpense(String auth, String amount) throws Exception {
        String body = mvc.perform(post("/api/transactions").header(AUTHORIZATION, auth).contentType(APPLICATION_JSON)
                        .content("""
                                {"amount": %s, "category": "FOOD", "description": "Lunch", "type": "EXPENSE", "date": "%s"}
                                """.formatted(amount, LocalDate.now())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asText();
    }

    @Test
    void transactionLifecycleAndStats() throws Exception {
        String auth = signUp();
        String month = YearMonth.now().toString();

        mvc.perform(post("/api/transactions").header(AUTHORIZATION, auth).contentType(APPLICATION_JSON)
                        .content("""
                                {"amount": 12.34, "category": "FOOD", "description": "Lunch", "type": "EXPENSE", "date": "%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(12.34))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.createdAt").exists());

        mvc.perform(get("/api/transactions").param("month", month).header(AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/stats/monthly").param("month", month).header(AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(12.34))
                .andExpect(jsonPath("$.byCategory.FOOD").value(12.34));

        mvc.perform(get("/api/stats/trend").header(AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(month));

        mvc.perform(get("/api/forecast").header(AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0)); // one month of data is not enough
    }

    @Test
    void usersCannotSeeOrTouchEachOthersTransactions() throws Exception {
        String alice = signUp();
        String bob = signUp();
        String id = createExpense(alice, "20");

        mvc.perform(delete("/api/transactions/{id}", id).header(AUTHORIZATION, bob))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
        mvc.perform(get("/api/transactions").header(AUTHORIZATION, bob))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/transactions/{id}", id).header(AUTHORIZATION, alice))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingOrBadTokensGet401() throws Exception {
        mvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Not authenticated"));
        mvc.perform(get("/api/transactions").header(AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void validationErrorsAreKeyedByField() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists())
                .andExpect(jsonPath("$.name").exists());

        String auth = signUp();
        mvc.perform(post("/api/transactions").header(AUTHORIZATION, auth).contentType(APPLICATION_JSON)
                        .content("{\"amount\":10,\"category\":\"CRYPTO\",\"type\":\"EXPENSE\",\"date\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.category").exists());
        mvc.perform(post("/api/transactions").header(AUTHORIZATION, auth).contentType(APPLICATION_JSON)
                        .content("{\"amount\":-5,\"category\":\"FOOD\",\"type\":\"EXPENSE\",\"date\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").exists());
        mvc.perform(get("/api/transactions").param("month", "2026-13").header(AUTHORIZATION, auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.month").exists());
        mvc.perform(get("/api/stats/trend").param("months", "0").header(AUTHORIZATION, auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.months").exists());
    }

    @Test
    void loginErrorsUseTheMessagesTheFrontendExpects() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
