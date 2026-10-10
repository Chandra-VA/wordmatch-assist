package com.wordmatch.assist.learning;

import com.wordmatch.assist.model.WordBox;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class PairTransitionDetectorTest {
    @Test
    public void detectsPairWhenReplacementWordsAlreadyAppeared() {
        List<WordBox> before = Arrays.asList(
                box("apple", 100), box("water", 100),
                box("苹果", 700), box("水", 700)
        );
        List<WordBox> duringReplacement = Arrays.asList(
                box("water", 100), box("水", 700),
                box("bread", 100), box("面包", 700)
        );

        assertTrue(PairTransitionDetector.didOppositePairDisappear(
                before,
                duringReplacement,
                1000
        ));
        List<WordBox> completed = PairTransitionDetector.findOppositePairDisappear(
                before,
                duringReplacement,
                1000
        );
        assertEquals("apple", completed.get(0).getNormalized());
        assertEquals("苹果", completed.get(1).getNormalized());
    }

    @Test
    public void rejectsTwoMissingWordsFromSameColumn() {
        List<WordBox> before = Arrays.asList(
                box("apple", 100), box("water", 100),
                box("苹果", 700), box("水", 700)
        );
        List<WordBox> current = Arrays.asList(
                box("苹果", 700), box("水", 700)
        );

        assertFalse(PairTransitionDetector.didOppositePairDisappear(before, current, 1000));
    }

    private static WordBox box(String text, int x) {
        return new WordBox(text, x, 300, x + 120, 350);
    }
}
