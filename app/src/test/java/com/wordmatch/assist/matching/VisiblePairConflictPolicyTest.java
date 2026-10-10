package com.wordmatch.assist.matching;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VisiblePairConflictPolicyTest {
    @Test
    public void removesWrongLearnedPairSeenOnTheSameBoard() {
        assertTrue(VisiblePairConflictPolicy.shouldRemove(
                "铁路",
                "gallery",
                "美术馆",
                "gallery",
                visible("铁路", "gallery", "美术馆", "railroad")
        ));
    }

    @Test
    public void preservesTheConfirmedPairItself() {
        assertFalse(VisiblePairConflictPolicy.shouldRemove(
                "gallery",
                "美术馆",
                "美术馆",
                "gallery",
                visible("铁路", "gallery", "美术馆", "railroad")
        ));
    }

    @Test
    public void preservesAlternativeTranslationNotOnThisBoard() {
        assertFalse(VisiblePairConflictPolicy.shouldRemove(
                "gallery",
                "画廊",
                "美术馆",
                "gallery",
                visible("gallery", "美术馆")
        ));
    }

    private static Set<String> visible(String... words) {
        Set<String> result = new HashSet<>();
        for (String word : Arrays.asList(words)) {
            result.add(WordNormalizer.normalize(word));
        }
        return result;
    }
}
