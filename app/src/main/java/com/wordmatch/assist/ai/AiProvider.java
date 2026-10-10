package com.wordmatch.assist.ai;

import java.util.Locale;

/** Provider-specific defaults for OpenAI-compatible Chat Completions APIs. */
public enum AiProvider {
    QWEN(
            "qwen",
            "Qwen（阿里云百炼）",
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            "qwen-flash",
            true
    ),
    DEEPSEEK(
            "deepseek",
            "DeepSeek",
            "https://api.deepseek.com/chat/completions",
            "deepseek-v4-flash",
            true
    ),
    OPENAI(
            "openai",
            "OpenAI",
            "https://api.openai.com/v1/chat/completions",
            "",
            true
    ),
    COMPATIBLE(
            "compatible",
            "其他 OpenAI 兼容接口",
            "",
            "",
            false
    );

    private final String id;
    private final String displayName;
    private final String defaultEndpoint;
    private final String defaultModel;
    private final boolean apiKeyRequired;

    AiProvider(
            String id,
            String displayName,
            String defaultEndpoint,
            String defaultModel,
            boolean apiKeyRequired
    ) {
        this.id = id;
        this.displayName = displayName;
        this.defaultEndpoint = defaultEndpoint;
        this.defaultModel = defaultModel;
        this.apiKeyRequired = apiKeyRequired;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public boolean isApiKeyRequired() {
        return apiKeyRequired;
    }

    public static AiProvider fromId(String id) {
        if (id != null) {
            for (AiProvider provider : values()) {
                if (provider.id.equalsIgnoreCase(id.trim())) {
                    return provider;
                }
            }
        }
        return COMPATIBLE;
    }

    /** Migrates settings created before the provider selector existed. */
    public static AiProvider detectFromEndpoint(String endpoint) {
        String clean = endpoint == null ? "" : endpoint.toLowerCase(Locale.ROOT);
        if (clean.contains("dashscope.aliyuncs.com") || clean.contains(".maas.aliyuncs.com")) {
            return QWEN;
        }
        if (clean.contains("api.deepseek.com")) {
            return DEEPSEEK;
        }
        if (clean.contains("api.openai.com")) {
            return OPENAI;
        }
        return COMPATIBLE;
    }

    /** Accepts either a provider base URL or a complete Chat Completions URL. */
    public String normalizeEndpoint(String endpoint) {
        String clean = endpoint == null ? "" : endpoint.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        if (clean.isEmpty() || clean.endsWith("/chat/completions")) {
            return clean;
        }
        switch (this) {
            case QWEN:
                if (clean.endsWith("/compatible-mode/v1")) {
                    return clean + "/chat/completions";
                }
                break;
            case DEEPSEEK:
                if (clean.equals("https://api.deepseek.com")
                        || clean.equals("https://api.deepseek.com/v1")) {
                    return clean + "/chat/completions";
                }
                break;
            case OPENAI:
                if (clean.equals("https://api.openai.com")
                        || clean.equals("https://api.openai.com/v1")) {
                    return clean + (clean.endsWith("/v1") ? "" : "/v1")
                            + "/chat/completions";
                }
                break;
            case COMPATIBLE:
            default:
                break;
        }
        return clean;
    }

    public boolean usesQwenThinkingParameter() {
        return this == QWEN;
    }

    public boolean usesDeepSeekThinkingParameter() {
        return this == DEEPSEEK;
    }

    /** Generic gateways often implement chat messages but not JSON mode. */
    public boolean usesJsonResponseFormat() {
        return this != COMPATIBLE;
    }
}
