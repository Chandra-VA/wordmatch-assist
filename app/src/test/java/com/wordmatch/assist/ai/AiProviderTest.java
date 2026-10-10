package com.wordmatch.assist.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AiProviderTest {
    @Test
    public void detectsExistingProviderEndpoints() {
        assertEquals(
                AiProvider.QWEN,
                AiProvider.detectFromEndpoint(
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                )
        );
        assertEquals(
                AiProvider.QWEN,
                AiProvider.detectFromEndpoint(
                        "https://workspace.cn-beijing.maas.aliyuncs.com/compatible-mode/v1"
                )
        );
        assertEquals(
                AiProvider.DEEPSEEK,
                AiProvider.detectFromEndpoint("https://api.deepseek.com/chat/completions")
        );
        assertEquals(
                AiProvider.OPENAI,
                AiProvider.detectFromEndpoint("https://api.openai.com/v1/chat/completions")
        );
        assertEquals(
                AiProvider.COMPATIBLE,
                AiProvider.detectFromEndpoint("https://models.example.com/v1/chat/completions")
        );
    }

    @Test
    public void normalizesKnownProviderBaseUrls() {
        assertEquals(
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                AiProvider.QWEN.normalizeEndpoint(
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/"
                )
        );
        assertEquals(
                "https://api.deepseek.com/chat/completions",
                AiProvider.DEEPSEEK.normalizeEndpoint("https://api.deepseek.com/")
        );
        assertEquals(
                "https://api.deepseek.com/v1/chat/completions",
                AiProvider.DEEPSEEK.normalizeEndpoint("https://api.deepseek.com/v1")
        );
        assertEquals(
                "https://api.openai.com/v1/chat/completions",
                AiProvider.OPENAI.normalizeEndpoint("https://api.openai.com")
        );
    }

    @Test
    public void leavesCustomEndpointPathUntouched() {
        assertEquals(
                "https://gateway.example.com/custom/chat/completions",
                AiProvider.COMPATIBLE.normalizeEndpoint(
                        "https://gateway.example.com/custom/chat/completions/"
                )
        );
    }

    @Test
    public void exposesProviderSpecificThinkingControls() {
        assertTrue(AiProvider.QWEN.usesQwenThinkingParameter());
        assertFalse(AiProvider.QWEN.usesDeepSeekThinkingParameter());
        assertTrue(AiProvider.DEEPSEEK.usesDeepSeekThinkingParameter());
        assertFalse(AiProvider.COMPATIBLE.usesQwenThinkingParameter());
        assertTrue(AiProvider.QWEN.usesJsonResponseFormat());
        assertTrue(AiProvider.DEEPSEEK.usesJsonResponseFormat());
        assertFalse(AiProvider.COMPATIBLE.usesJsonResponseFormat());
    }
}
