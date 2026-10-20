package com.company.incidentportal.service;

import com.company.incidentportal.ai.ClaudeClient;
import com.company.incidentportal.dto.AiSuggestionResponse;
import com.company.incidentportal.dto.IncidentResponse;
import com.company.incidentportal.entity.Roles;
import com.company.incidentportal.exception.AiUnavailableException;
import com.company.incidentportal.exception.ForbiddenException;
import com.company.incidentportal.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private ClaudeClient claude;

    @Mock
    private IncidentService incidentService;

    private AiService service;
    private final AuthenticatedUser actor = new AuthenticatedUser(2L, "alice", Roles.USER);

    @BeforeEach
    void setUp() {
        service = new AiService(claude, incidentService, new ObjectMapper());
    }

    // ---------- suggest ----------

    @Test
    void shouldParseSuggestionEvenWhenWrappedInCodeFence() {
        when(claude.complete(anyString(), anyString(), anyInt())).thenReturn(
                "```json\n{\"priority\":\"high\",\"category\":\"Database Outage\",\"reason\":\"DB is down\"}\n```");

        AiSuggestionResponse result = service.suggest("DB down", "cannot connect");

        assertEquals("HIGH", result.priority());
        assertEquals("Database Outage", result.category());
        assertEquals("DB is down", result.reason());
    }

    @Test
    void shouldRejectUnknownPriorityMissingCategoryAndNonJson() {
        when(claude.complete(anyString(), anyString(), anyInt())).thenReturn(
                "{\"priority\":\"URGENT\",\"category\":\"X\"}",
                "{\"priority\":\"LOW\",\"category\":\"\"}",
                "Sorry, I cannot help with that",
                "{ not valid json }");

        assertThrows(AiUnavailableException.class, () -> service.suggest("t", "d"));
        assertThrows(AiUnavailableException.class, () -> service.suggest("t", "d"));
        assertThrows(AiUnavailableException.class, () -> service.suggest("t", "d"));
        assertThrows(AiUnavailableException.class, () -> service.suggest("t", "d"));
    }

    @Test
    void shouldTrimOverlongCategory() {
        String longCategory = "x".repeat(80);
        when(claude.complete(anyString(), anyString(), anyInt())).thenReturn(
                "{\"priority\":\"LOW\",\"category\":\"" + longCategory + "\"}");

        assertEquals(50, service.suggest("t", "d").category().length());
    }

    // ---------- summarize ----------

    @Test
    void shouldSummarizeVisibleIncident() {
        when(incidentService.getIncidentById(5L, actor)).thenReturn(incident(5L, "Payments down"));
        when(claude.complete(anyString(), anyString(), anyInt())).thenReturn("  Payments are down.  ");

        assertEquals("Payments are down.", service.summarize(5L, actor).summary());
    }

    @Test
    void shouldNotCallAiWhenUserMayNotSeeIncident() {
        when(incidentService.getIncidentById(5L, actor)).thenThrow(new ForbiddenException("no"));

        assertThrows(ForbiddenException.class, () -> service.summarize(5L, actor));
        verify(claude, never()).complete(anyString(), anyString(), anyInt());
    }

    @Test
    void shouldRejectBlankSummary() {
        when(incidentService.getIncidentById(5L, actor)).thenReturn(incident(5L, "x"));
        when(claude.complete(anyString(), anyString(), anyInt())).thenReturn("   ");

        assertThrows(AiUnavailableException.class, () -> service.summarize(5L, actor));
    }

    // ---------- similar ----------

    @Test
    void shouldReturnOnlyKnownDistinctIdsUpToThree() {
        when(incidentService.getIncidents(null, null, null, actor)).thenReturn(
                List.of(incident(1L, "a"), incident(2L, "b"), incident(3L, "c"), incident(4L, "d")));
        when(claude.complete(anyString(), anyString(), anyInt()))
                .thenReturn("{\"ids\":[2,99,2,1,3,4]}");

        List<IncidentResponse> result = service.findSimilar("t", "d", actor);

        assertEquals(List.of(2L, 1L, 3L), result.stream().map(IncidentResponse::getId).toList());
    }

    @Test
    void shouldSkipAiWhenThereAreNoIncidentsToCompare() {
        when(incidentService.getIncidents(null, null, null, actor)).thenReturn(List.of());

        assertTrue(service.findSimilar("t", "d", actor).isEmpty());
        verify(claude, never()).complete(anyString(), anyString(), anyInt());
    }

    @Test
    void shouldReportEnabledFlag() {
        when(claude.isConfigured()).thenReturn(true);

        assertTrue(service.isEnabled());
    }

    private IncidentResponse incident(Long id, String title) {
        IncidentResponse r = new IncidentResponse();
        r.setId(id);
        r.setTitle(title);
        r.setDescription("desc");
        r.setApplication("Billing");
        r.setCategory("Outage");
        r.setPriority("HIGH");
        r.setStatus("OPEN");
        r.setCreatedByUsername("alice");
        return r;
    }
}
