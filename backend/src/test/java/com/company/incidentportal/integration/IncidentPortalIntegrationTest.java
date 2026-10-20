package com.company.incidentportal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end API test (security + controllers + services + JPA on in-memory H2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentPortalIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static final String INCIDENT_JSON = """
            {"title":"Payments API down","description":"500 errors","application":"Payments",
             "priority":"HIGH","category":"Outage"}""";

    @Test
    void loginRejectsBadCredentialsAndAcceptsValidOnes() throws Exception {
        login("admin", "wrong").andExpect(status().isUnauthorized());
        login("admin", "Admin@12345").andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/incidents").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullIncidentLifecycleWithRoles() throws Exception {
        String admin = token("admin", "Admin@12345");
        String user = token("user", "User@12345");
        long userId = userId(admin, "user");

        // user creates
        long id = createIncident(user, INCIDENT_JSON);

        // validation
        mvc.perform(post("/api/incidents").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.priority").exists());

        // user cannot assign or delete (admin only)
        mvc.perform(put("/api/incidents/" + id + "/assign").param("userId", "" + userId)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/incidents/" + id).header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());

        // user cannot change status of OPEN incident they only created
        mvc.perform(put("/api/incidents/" + id + "/status").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());

        // admin assigns to user -> user (assignee) can progress it
        mvc.perform(put("/api/incidents/" + id + "/assign").param("userId", "" + userId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToUsername").value("user"));
        setStatus(user, id, "IN_PROGRESS").andExpect(status().isOk());
        setStatus(user, id, "CLOSED").andExpect(status().isConflict());   // skipping RESOLVED
        setStatus(user, id, "RESOLVED").andExpect(status().isOk());
        setStatus(admin, id, "CLOSED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // closed incidents cannot be modified
        mvc.perform(put("/api/incidents/" + id).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(INCIDENT_JSON))
                .andExpect(status().isConflict());

        // filters
        mvc.perform(get("/api/incidents").param("status", "CLOSED").param("priority", "high")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")]").exists());
        mvc.perform(get("/api/incidents").param("status", "OPEN").param("application", "Payments")
                        .header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[?(@.id==" + id + ")]").isEmpty());

        // soft delete hides it
        mvc.perform(delete("/api/incidents/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/incidents/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersOnlySeeTheirOwnIncidentsAndCreatorCanCloseResolved() throws Exception {
        String admin = token("admin", "Admin@12345");
        String user = token("user", "User@12345");

        long adminIncident = createIncident(admin, INCIDENT_JSON);
        long userIncident = createIncident(user, INCIDENT_JSON);

        mvc.perform(get("/api/incidents/" + adminIncident).header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/incidents").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + adminIncident + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id==" + userIncident + ")]").exists());

        // creator may close only after an admin resolved it
        setStatus(admin, userIncident, "IN_PROGRESS").andExpect(status().isOk());
        setStatus(admin, userIncident, "RESOLVED").andExpect(status().isOk());
        setStatus(user, userIncident, "CLOSED").andExpect(status().isOk());
    }

    @Test
    void adminManagesUsers() throws Exception {
        String admin = token("admin", "Admin@12345");
        String user = token("user", "User@12345");

        mvc.perform(get("/api/users").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"email\":\"dave@example.com\",\"password\":\"password123\",\"role\":\"user\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"email\":\"dave@example.com\",\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isConflict());

        // email must be on the company domain, valid and unique
        mvc.perform(post("/api/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"erin\",\"email\":\"erin@gmail.com\",\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("company domain")));
        mvc.perform(post("/api/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"erin\",\"email\":\"not-an-email\",\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"erin\",\"email\":\"DAVE@example.com\",\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isConflict());

        login("dave", "password123").andExpect(status().isOk());

        long daveId = userId(admin, "dave");
        mvc.perform(put("/api/users/" + daveId + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
    }

    // ---------- helpers ----------

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private String token(String username, String password) throws Exception {
        String body = login(username, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private long createIncident(String token, String json) throws Exception {
        String body = mvc.perform(post("/api/incidents").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    private ResultActions setStatus(String token, long id, String newStatus) throws Exception {
        return mvc.perform(put("/api/incidents/" + id + "/status")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + newStatus + "\"}"));
    }

    private long userId(String adminToken, String username) throws Exception {
        String body = mvc.perform(get("/api/users").header("Authorization", bearer(adminToken)))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode n : mapper.readTree(body)) {
            if (username.equals(n.get("username").asText())) {
                return n.get("id").asLong();
            }
        }
        throw new IllegalStateException("user not found " + username);
    }
}
