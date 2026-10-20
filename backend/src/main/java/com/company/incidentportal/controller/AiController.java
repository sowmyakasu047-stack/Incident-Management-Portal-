package com.company.incidentportal.controller;

import com.company.incidentportal.dto.AiSuggestionResponse;
import com.company.incidentportal.dto.AiSummaryResponse;
import com.company.incidentportal.dto.AiTextRequest;
import com.company.incidentportal.dto.IncidentResponse;
import com.company.incidentportal.security.AuthenticatedUser;
import com.company.incidentportal.service.AiService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** AI helpers for logged-in users. Everything returns 503 when the AI is not configured/available. */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // Lets the UI hide the AI buttons when no API key is set
    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", aiService.isEnabled());
    }

    @PostMapping("/suggest")
    public AiSuggestionResponse suggest(@Valid @RequestBody AiTextRequest request) {
        return aiService.suggest(request.title(), request.description());
    }

    @PostMapping("/similar")
    public List<IncidentResponse> similar(@Valid @RequestBody AiTextRequest request,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return aiService.findSimilar(request.title(), request.description(), user);
    }

    @PostMapping("/incidents/{id}/summary")
    public AiSummaryResponse summarize(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return aiService.summarize(id, user);
    }
}
