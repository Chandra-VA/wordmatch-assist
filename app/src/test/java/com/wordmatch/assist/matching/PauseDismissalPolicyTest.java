package com.wordmatch.assist.matching;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PauseDismissalPolicyTest {
    @Test
    public void neverClicksThroughAVisibleDialogEvenWhenBoardNodesAreReadable() {
        assertFalse(PauseDismissalPolicy.isReady(true, 10, true, 500L, 96L, 240L));
    }

    @Test
    public void waitsForDialogAbsenceToRemainStable() {
        assertFalse(PauseDismissalPolicy.isReady(false, 10, true, 16L, 96L, 240L));
    }

    @Test
    public void releasesAfterStableDialogFreeBoard() {
        assertTrue(PauseDismissalPolicy.isReady(false, 10, true, 96L, 96L, 240L));
    }

    @Test
    public void doesNotReleaseOntoAnEmptyTransitionFrame() {
        assertFalse(PauseDismissalPolicy.isReady(false, 0, false, 120L, 96L, 240L));
    }

    @Test
    public void backgroundWordsWithoutLessonTitleCannotConfirmDialogDismissal() {
        assertFalse(PauseDismissalPolicy.isReady(false, 10, false, 1000L, 96L, 240L));
        assertFalse(PauseDismissalPolicy.isReady(false, 2, false, 1000L, 96L, 240L));
    }

    @Test
    public void finalPairCanResumeWhenTheMatchingTitleIsVisible() {
        assertTrue(PauseDismissalPolicy.isReady(false, 2, true, 96L, 96L, 240L));
    }

    @Test
    public void eventuallyReleasesAnEmptyRoundCompletionScreen() {
        assertTrue(PauseDismissalPolicy.isReady(false, 0, false, 240L, 96L, 240L));
    }
}
