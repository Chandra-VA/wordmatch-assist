package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SubmittedBoardEvidenceTest {
    private final WordBox left = new WordBox("接受", 180, 520, 220, 545);
    private final WordBox right = new WordBox("accept", 570, 520, 630, 545);
    private final List<MatchPair> pairs = Collections.singletonList(new MatchPair(left, right, 1.0, 1));
    @Test public void allGreyTailRequiresEveryVisibleWordToHaveActuallyBeenSubmitted() {
        assertTrue(SubmittedBoardEvidence.covers(Arrays.asList(left, right), pairs));
        WordBox untouched = new WordBox("pick", 570, 620, 630, 645);
        assertFalse(SubmittedBoardEvidence.covers(Arrays.asList(left, right, untouched), pairs));
        assertFalse(SubmittedBoardEvidence.covers(Arrays.asList(left, right), Collections.emptyList()));
    }
    @Test public void sameTextElsewhereReplacementAndEmptyBoardAreNotCoverage() {
        WordBox duplicate = new WordBox("接受", 180, 620, 220, 645);
        WordBox replacement = new WordBox("讲座", 180, 520, 220, 545);
        assertFalse(SubmittedBoardEvidence.covers(Arrays.asList(duplicate, right), pairs));
        assertFalse(SubmittedBoardEvidence.covers(Arrays.asList(replacement, right), pairs));
        assertFalse(SubmittedBoardEvidence.covers(Collections.emptyList(), pairs));
        assertFalse(SubmittedBoardEvidence.covers(Collections.singletonList(left), pairs));
        assertFalse(SubmittedBoardEvidence.covers(null, pairs));
    }
}
