package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public final class PairMatcherTest {
    @Test
    public void choosesOneToOnePairsAcrossColumns() {
        Map<String, String> pairs = new HashMap<>();
        pairs.put("apple", "苹果");
        pairs.put("water", "水");
        TranslationLookup lookup = (first, second) -> {
            String target = pairs.get(first);
            if (second.equals(target)) {
                return 1.0;
            }
            target = pairs.get(second);
            return first.equals(target) ? 1.0 : 0.0;
        };

        List<WordBox> words = Arrays.asList(
                new WordBox("apple", 100, 300, 260, 350),
                new WordBox("水", 700, 300, 800, 350),
                new WordBox("water", 100, 400, 260, 450),
                new WordBox("苹果", 700, 400, 800, 450)
        );
        List<MatchPair> matches = new PairMatcher(lookup).match(words, 1000);

        assertEquals(2, matches.size());
        assertEquals("apple", matches.get(0).getFirst().getNormalized());
        assertEquals("苹果", matches.get(0).getSecond().getNormalized());
    }

    @Test
    public void refusesAmbiguousExactEdgesFromCorruptedLearning() {
        Set<String> edges = new HashSet<>(Arrays.asList(
                key("独白", "solo"),
                key("美术馆", "gallery"),
                key("铁路", "railroad"),
                key("铁路", "gallery")
        ));
        TranslationLookup lookup = (first, second) -> edges.contains(key(first, second))
                ? 1.0
                : 0.0;
        List<WordBox> words = Arrays.asList(
                new WordBox("独白", 100, 300, 260, 350),
                new WordBox("solo", 700, 300, 860, 350),
                new WordBox("美术馆", 100, 400, 260, 450),
                new WordBox("gallery", 700, 400, 860, 450),
                new WordBox("铁路", 100, 500, 260, 550),
                new WordBox("railroad", 700, 500, 860, 550)
        );

        List<MatchPair> matches = new PairMatcher(lookup).match(words, 1000);

        assertEquals(1, matches.size());
        assertEquals("独白", matches.get(0).getFirst().getNormalized());
        assertEquals("solo", matches.get(0).getSecond().getNormalized());
    }

    @Test
    public void pairsDuplicateIdenticalCardsArbitrarilyOneToOne() {
        TranslationLookup lookup = (first, second) -> key("车费", "fare")
                .equals(key(first, second)) ? 1.0 : 0.0;
        List<WordBox> words = Arrays.asList(
                new WordBox("车费", 100, 300, 260, 350),
                new WordBox("fare", 700, 300, 860, 350),
                new WordBox("车费", 100, 400, 260, 450),
                new WordBox("fare", 700, 400, 860, 450)
        );

        List<MatchPair> matches = new PairMatcher(lookup).match(words, 1000);

        assertEquals(2, matches.size());
        assertEquals("车费", matches.get(0).getFirst().getNormalized());
        assertEquals("fare", matches.get(0).getSecond().getNormalized());
        assertEquals("车费", matches.get(1).getFirst().getNormalized());
        assertEquals("fare", matches.get(1).getSecond().getNormalized());
    }

    private static String key(String first, String second) {
        return first.compareTo(second) <= 0
                ? first + "\u0000" + second
                : second + "\u0000" + first;
    }
}
