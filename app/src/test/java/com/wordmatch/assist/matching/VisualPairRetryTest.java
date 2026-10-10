package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class VisualPairRetryTest {
    private final MatchPair pair = pair("凌乱", "messy", 400);

    @Test public void permitsOnlyFreshMatchingWordsAndLocations() {
        VisualPairRetry retry = new VisualPairRetry();
        assertFalse(retry.permits(pair, 100));
        assertTrue(retry.authorize(pair, 100));
        assertTrue(retry.permits(pair, 101));
        assertTrue(retry.permits(pair.getSecond(), 1599));
        assertFalse(retry.permits(pair("凌乱", "messy", 500), 101));
        assertFalse(retry.permits(pair("结果", "result", 400), 101));
        assertFalse(retry.permits(pair, 1600));
    }

    @Test public void cancellationRevokesPermissionButDoesNotResetAttemptLimit() {
        VisualPairRetry retry = new VisualPairRetry();
        assertTrue(retry.authorize(pair, 100));
        retry.clearActive();
        assertFalse(retry.permits(pair, 101));
        assertTrue(retry.authorize(pair, 200));
        retry.clearActive();
        assertFalse(retry.canRetry(pair));
        assertFalse(retry.authorize(pair, 300));
        assertTrue(retry.authorize(pair("职业", "career", 600), 300));
    }

    @Test public void emptyOrHalfMissingBoardDoesNotResetAttemptLimit() {
        VisualPairRetry retry = new VisualPairRetry();
        retry.authorize(pair, 100);
        retry.authorize(pair, 200);
        retry.pruneMissing(Collections.emptyList());
        retry.pruneMissing(Arrays.asList(pair.getFirst(), new WordBox("new", 700, 800, 750, 830)));
        assertFalse(retry.canRetry(pair));
        MatchPair replacement = pair("职业", "career", 600);
        retry.pruneMissing(Arrays.asList(replacement.getFirst(), replacement.getSecond()));
        assertTrue(retry.canRetry(pair));
    }

    @Test public void reversedPairUsesSameAttemptLimit() {
        VisualPairRetry retry = new VisualPairRetry();
        retry.authorize(pair, 0);
        MatchPair reverse = new MatchPair(pair.getSecond(), pair.getFirst(), 1, 2);
        assertTrue(retry.permits(reverse, 1));
        retry.authorize(reverse, 100);
        assertFalse(retry.canRetry(pair));
        retry.reset();
        assertTrue(retry.canRetry(pair));
        assertFalse(retry.permits(pair, 200));
    }

    private static MatchPair pair(String left, String right, int y) {
        return new MatchPair(new WordBox(left, 100, y, 150, y + 30),
                new WordBox(right, 700, y, 750, y + 30), 1, 1);
    }
}
