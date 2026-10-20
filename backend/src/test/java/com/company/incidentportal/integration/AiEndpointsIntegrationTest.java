package com.company.incidentportal.integration;

import com.company.incidentportal.ai.ClaudeClient;
import com.company.incidentportal.exception.AiUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AI endpoints with the Claude client mocked, so no API key or network is needed. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:aitest;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiEndpointsIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private ClaudeClient claude;

    private static final String BODY = "{\"title\":\"DB down\",\"description\":\"cannot connect\"}";

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"User@12345\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("token").asText();
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(post("/api/ai/suggest").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reportsStatus() throws Exception {
        when(claude.isConfigured()).thenReturn(true);

        mvc.perform(get("/api/ai/status").header("Authorization", token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void suggestsPriorityAndCategory() throws Exception {
        when(claude.complete(anyString(), anyString(), anyInt()))
                .thenReturn("{\"priority\":\"CRITICAL\",\"category\":\"Outage\",\"reason\":\"all users affected\"}");

        mvc.perform(post("/api/ai/suggest").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.category").value("Outage"));
    }

    @Test
    void validatesInputAndReturns503WhenAiIsDown() throws Exception {
        mvc.perform(post("/api/ai/suggest").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest());

        when(claude.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new AiUnavailableException("The AI service is temporarily unavailable"));

        mvc.perform(post("/api/ai/suggest").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The AI service is temporarily unavailable"));
    }

    @Test
    void summaryOfMissingIncidentIs404() throws Exception {
        mvc.perform(post("/api/ai/incidents/99999/summary").header("Authorization", token()))
                .andExpect(status().isNotFound());
    }
}
