package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;

public final class ExactPairSelectorTest {
    private final WordBox left = new WordBox("apple", 100, 300, 260, 350);
    private final WordBox right = new WordBox("苹果", 700, 300, 800, 350);

    @Test
    public void acceptsExactVocabularyPair() {
        MatchPair exact = new MatchPair(left, right, 1.0, 1);

        assertSame(exact, ExactPairSelector.firstExact(Collections.singletonList(exact)));
    }

    @Test
    public void skipsFuzzyPairAndFindsNextExactPair() {
        MatchPair fuzzy = new MatchPair(left, right, 0.91, 1);
        MatchPair exact = new MatchPair(left, right, 1.0, 2);

        assertSame(exact, ExactPairSelector.firstExact(Arrays.asList(fuzzy, exact)));
        assertNull(ExactPairSelector.firstExact(Collections.singletonList(fuzzy)));
    }

    @Test
    public void returnsEveryExactPairForBurstQueue() {
        MatchPair fuzzy = new MatchPair(left, right, 0.93, 1);
        MatchPair first = new MatchPair(left, right, 1.0, 2);
        MatchPair second = new MatchPair(left, right, 1.0, 3);

        assertEquals(
                Arrays.asList(first, second),
                ExactPairSelector.allExact(Arrays.asList(fuzzy, first, second))
        );
    }
}
