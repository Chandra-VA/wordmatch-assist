package com.wordmatch.assist.matching;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class WordNormalizerTest {
    @Test
    public void normalizesOcrPunctuationAndWhitespace() {
        assertEquals("i'm here", WordNormalizer.normalize("  “I’m   here!” "));
    }

    @Test
    public void similarityHandlesOneOcrMistake() {
        assertTrue(WordNormalizer.similarity("apple", "appIe") >= 0.8);
    }
}
