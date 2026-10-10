package com.wordmatch.assist.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AiTransportPolicyTest {
    private static final String ENDPOINT = "https://example.com/v1/chat/completions";

    @Test public void acceptsHttpsEndpointAndExplicitSecurePort() {
        assertTrue(AiTransportPolicy.isValidEndpoint(ENDPOINT));
        assertTrue(AiTransportPolicy.isValidEndpoint("https://example.com:8443/chat/completions"));
    }
    @Test public void rejectsEmbeddedCredentialsQueriesAndInvalidUrls() {
        for (String endpoint : new String[] {null, "", "http://example.com/chat/completions",
                "https://user:pass@example.com/chat/completions", ENDPOINT + "?key=test",
                ENDPOINT + "#fragment", "https:///chat/completions", "https://example.com:0/chat/completions"}) {
            assertFalse(AiTransportPolicy.isValidEndpoint(endpoint));
        }
    }
    @Test public void reusesKeysOnlyOnTheSameSecureOrigin() {
        assertTrue(AiTransportPolicy.canReuseKey(ENDPOINT, "https://EXAMPLE.com:443/v2/chat/completions"));
        assertFalse(AiTransportPolicy.canReuseKey(ENDPOINT, "https://other.example/chat/completions"));
        assertFalse(AiTransportPolicy.canReuseKey(ENDPOINT, "https://example.com:8443/chat/completions"));
        assertFalse(AiTransportPolicy.canReuseKey(ENDPOINT, "http://example.com/chat/completions"));
    }
    @Test public void providerErrorsCannotEchoTheCurrentCredential() {
        String dummy = "dummy-test-credential";
        assertFalse(AiTransportPolicy.redact("Rejected " + dummy, dummy).contains(dummy));
        assertEquals("Bearer [REDACTED]", AiTransportPolicy.redact("Bearer dummy", ""));
        assertEquals("rate limit", AiTransportPolicy.redact("rate limit", dummy));
    }
}
