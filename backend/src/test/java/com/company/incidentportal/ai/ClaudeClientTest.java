package com.company.incidentportal.ai;

import com.company.incidentportal.exception.AiUnavailableException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeClientTest {

    @Test
    void shouldBeDisabledAndRefuseCallsWithoutApiKey() {
        ClaudeClient client = new ClaudeClient("https://api.anthropic.com", "  ", "model", 5);

        assertFalse(client.isConfigured());
        assertThrows(AiUnavailableException.class, () -> client.complete("sys", "user", 10));
    }

    @Test
    void shouldBeEnabledWhenApiKeyIsSet() {
        assertTrue(new ClaudeClient("https://api.anthropic.com", "key", "model", 5).isConfigured());
    }
}
