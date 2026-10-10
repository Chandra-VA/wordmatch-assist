package com.wordmatch.assist.ai;

import java.net.URI;
import java.net.URISyntaxException;

/** Limits credentials to an explicitly configured HTTPS origin. */
public final class AiTransportPolicy {
    private AiTransportPolicy() { }

    public static boolean isValidEndpoint(String value) {
        URI uri = parse(value);
        return uri != null && "https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null && !uri.getHost().isEmpty()
                && uri.getRawUserInfo() == null && uri.getRawQuery() == null
                && uri.getRawFragment() == null && uri.getPath() != null
                && uri.getPath().endsWith("/chat/completions")
                && (uri.getPort() == -1 || (uri.getPort() > 0 && uri.getPort() <= 65535));
    }

    public static boolean canReuseKey(String savedEndpoint, String newEndpoint) {
        if (!isValidEndpoint(savedEndpoint) || !isValidEndpoint(newEndpoint)) {
            return false;
        }
        URI saved = parse(savedEndpoint);
        URI next = parse(newEndpoint);
        return saved.getHost().equalsIgnoreCase(next.getHost())
                && port(saved) == port(next);
    }

    public static String redact(String message, String apiKey) {
        String clean = message == null ? "" : message;
        if (apiKey != null && !apiKey.isEmpty()) {
            clean = clean.replace(apiKey, "[REDACTED]");
        }
        return clean.replaceAll("(?i)Bearer\\s+[^\\s\"'<>]+", "Bearer [REDACTED]");
    }

    private static int port(URI uri) {
        return uri.getPort() == -1 ? 443 : uri.getPort();
    }

    private static URI parse(String value) {
        if (value == null) return null;
        try {
            return new URI(value);
        } catch (URISyntaxException error) {
            return null;
        }
    }
}
