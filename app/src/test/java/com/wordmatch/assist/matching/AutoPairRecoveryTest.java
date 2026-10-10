package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.wordmatch.assist.matching.AutoPairRecovery.Action.*;
import static org.junit.Assert.*;

public final class AutoPairRecoveryTest {
    private static final int W = 1000, H = 1600;
    private static final MatchPair A = pair("有点", "a bit", 300);
    private static final MatchPair B = pair("地毯", "carpet", 500);
    private static final List<MatchPair> PAIRS = Arrays.asList(A, B);
    private static final List<WordBox> NONE = Collections.emptyList();

    @Test public void pausedTimeDoesNotExpireReviewOrEraseFailureLimits() {
        AutoPairRecovery recovery = exhausted(A);
        recovery.deferForPause(1500);
        assertEquals(WAIT, evaluate(recovery, PAIRS, NONE, 1999));
        assertEquals(Collections.singletonList(B), recovery.availablePairs(PAIRS, W, H));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 2000));

        recovery.beginReview(3000);
        recovery.deferForPause(1500);
        assertEquals(WAIT, recovery.evaluate(Collections.emptyList(), NONE, true, true, W, H, 9000));
        assertEquals(WAIT, recovery.evaluate(Collections.emptyList(), NONE, true, true, W, H, 10499));
        assertEquals(TAKEOVER, recovery.evaluate(Collections.emptyList(), NONE, true, true, W, H, 10500));
    }

    @Test
    public void threeFailuresOnOnePairDoNotStopAnotherHighlightedPair() {
        AutoPairRecovery recovery = exhausted(A);
        assertEquals(Collections.singletonList(B), recovery.availablePairs(PAIRS, W, H));
        assertEquals(WAIT, evaluate(recovery, PAIRS, NONE, 499));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 500));
        assertFalse(recovery.isPending());
    }

    @Test
    public void failuresOnDifferentPairsAreNotAddedToOneGlobalStopCounter() {
        AutoPairRecovery recovery = new AutoPairRecovery();
        recovery.recordFailure(A, W, H, 0);
        recovery.recordFailure(A, W, H, 0);
        recovery.recordFailure(B, W, H, 0);
        assertEquals(Arrays.asList(B, A), recovery.availablePairs(PAIRS, W, H));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 500));
    }

    @Test
    public void selectedFailedCardIsClearedBeforeAnUnrelatedPairCanStart() {
        AutoPairRecovery recovery = exhausted(A);
        List<WordBox> selectedA = Collections.singletonList(A.getSecond());
        assertEquals(CLEAR_SELECTION, evaluate(recovery, PAIRS, selectedA, 500));
        assertEquals(WAIT, evaluate(recovery, PAIRS, NONE, 999));
        assertEquals(WAIT, evaluate(recovery, PAIRS, selectedA, 1000));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 1080));
    }

    @Test
    public void unsuccessfulDeselectionIsNotRepeatedAndEventuallyRequestsHelp() {
        AutoPairRecovery recovery = exhausted(A);
        List<WordBox> selectedA = Collections.singletonList(A.getSecond());
        assertEquals(CLEAR_SELECTION, evaluate(recovery, PAIRS, selectedA, 500));
        assertEquals(WAIT, evaluate(recovery, PAIRS, selectedA, 1000));
        assertEquals(WAIT, evaluate(recovery, PAIRS, selectedA, 5900));
        assertEquals(TAKEOVER, evaluate(recovery, PAIRS, selectedA, 6000));
        assertFalse(recovery.isPending());
    }

    @Test
    public void selectedPairWithRetriesRemainingFinishesItsPartnerFirst() {
        AutoPairRecovery recovery = new AutoPairRecovery();
        recovery.recordFailure(A, W, H, 0);
        List<WordBox> selectedA = Collections.singletonList(A.getSecond());
        assertEquals(RESUME, evaluate(recovery, PAIRS, selectedA, 500));
        SelectedPairPlanner.Plan plan = SelectedPairPlanner.choose(
                recovery.availablePairs(PAIRS, W, H), selectedA, W, H);
        assertNotNull(plan);
        assertSame(A.getFirst(), plan.getPair().getSecond());
        assertTrue(plan.isFirstAlreadySelected());
    }

    @Test
    public void twoSelectedCardsWaitForFeedbackWithoutClickingAnotherPair() {
        AutoPairRecovery recovery = exhausted(A);
        assertEquals(WAIT, evaluate(recovery, PAIRS,
                Arrays.asList(A.getFirst(), A.getSecond()), 500));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 800));
    }

    @Test
    public void incompleteScanCannotCauseTakeoverOrUnrelatedClicksEvenAfterDeadline() {
        AutoPairRecovery recovery = exhausted(A);
        assertEquals(WAIT, recovery.evaluate(PAIRS, NONE, false, true, W, H, 10000));
        assertEquals(WAIT, recovery.evaluate(PAIRS, Collections.singletonList(A.getFirst()),
                false, true, W, H, 11000));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 12000));
    }

    @Test
    public void disabledKnownCardsWaitWithoutInvokingTranslationOrTakeoverImmediately() {
        AutoPairRecovery recovery = new AutoPairRecovery();
        recovery.beginReview(0);
        assertEquals(WAIT, evaluate(recovery, Collections.emptyList(), NONE, 500));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 800));
    }

    @Test
    public void exhaustedBoardIsRecheckedBeforeTakeover() {
        AutoPairRecovery recovery = exhausted(A);
        assertTrue(recovery.isPending());
        assertEquals(WAIT, evaluate(recovery, Collections.singletonList(A), NONE, 500));
        assertEquals(WAIT, evaluate(recovery, Collections.singletonList(A), NONE, 5999));
        assertEquals(TAKEOVER, evaluate(recovery, Collections.singletonList(A), NONE, 6000));
    }

    @Test
    public void successOnAnotherPairDoesNotReenableAnExhaustedPair() {
        AutoPairRecovery recovery = exhausted(A);
        recovery.recordFailure(B, W, H, 0);
        recovery.recordSuccess(B, W, H);
        assertEquals(Collections.singletonList(B), recovery.availablePairs(PAIRS, W, H));
        recovery.recordSuccess(A, W, H);
        assertEquals(PAIRS, recovery.availablePairs(PAIRS, W, H));
    }

    @Test
    public void emptyOrPartiallyDisappearingBoardsDoNotResetFailedPair() {
        AutoPairRecovery recovery = exhausted(A);
        recovery.pruneMissing(NONE, W, H);
        recovery.pruneMissing(Arrays.asList(A.getFirst(), B.getFirst(), B.getSecond()), W, H);
        assertEquals(Collections.singletonList(B), recovery.availablePairs(PAIRS, W, H));
        recovery.pruneMissing(Arrays.asList(B.getFirst(), B.getSecond()), W, H);
        assertEquals(PAIRS, recovery.availablePairs(PAIRS, W, H));
    }

    @Test
    public void reversedClickOrderStillCountsFailuresForTheSamePair() {
        AutoPairRecovery recovery = new AutoPairRecovery();
        recovery.recordFailure(A, W, H, 0);
        recovery.recordFailure(new MatchPair(A.getSecond(), A.getFirst(), 1.0, 1), W, H, 0);
        recovery.recordFailure(A, W, H, 0);
        assertEquals(Collections.singletonList(B), recovery.availablePairs(PAIRS, W, H));
        MatchPair repeatedTextElsewhere = pair("有点", "a bit", 800);
        assertEquals(Collections.singletonList(repeatedTextElsewhere),
                recovery.availablePairs(Arrays.asList(A, repeatedTextElsewhere), W, H));
    }

    @Test
    public void manualInterventionResetsFailureLimits() {
        AutoPairRecovery recovery = exhausted(A);
        recovery.reset();
        assertFalse(recovery.isPending());
        assertEquals(PAIRS, recovery.availablePairs(PAIRS, W, H));
    }

    @Test
    public void disappearanceAllowsNormalPlanningOfRemainingUnknownWords() {
        AutoPairRecovery recovery = exhausted(A);
        recovery.pruneMissing(Arrays.asList(B.getFirst(), B.getSecond()), W, H);
        assertEquals(RESUME, recovery.evaluate(Collections.emptyList(), NONE, true, false, W, H, 500));
    }

    @Test
    public void fuzzyHighlightIsNotPromotedToAnAutomaticClickDuringRecovery() {
        AutoPairRecovery recovery = exhausted(A);
        MatchPair fuzzy = new MatchPair(B.getFirst(), B.getSecond(), 0.91, 2);
        assertTrue(recovery.availablePairs(Arrays.asList(A, fuzzy), W, H).isEmpty());
        assertEquals(WAIT, evaluate(recovery, Arrays.asList(A, fuzzy), NONE, 500));
    }

    @Test
    public void missingSelectionFlagAfterOnlyOneClickDoesNotAllowAnotherPair() {
        AutoPairRecovery recovery = new AutoPairRecovery();
        recovery.recordFailure(A, W, H, 0, true);
        assertEquals(Collections.singletonList(A), recovery.availablePairs(PAIRS, W, H));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 500));
        assertEquals(Collections.singletonList(A), recovery.availablePairs(PAIRS, W, H));
    }

    @Test
    public void explicitSelectionCanRecoverAnExhaustedHalfClickedPair() {
        AutoPairRecovery recovery = exhausted(A);
        recovery.recordFailure(A, W, H, 0, true);
        assertEquals(WAIT, evaluate(recovery, PAIRS, NONE, 500));
        assertEquals(CLEAR_SELECTION, evaluate(recovery, PAIRS,
                Collections.singletonList(A.getFirst()), 600));
        assertEquals(RESUME, evaluate(recovery, PAIRS, NONE, 1100));
    }

    @Test
    public void visualReadinessUnblocksOnlyThatPairAndClearsStalePartialSelection() {
        AutoPairRecovery recovery = exhausted(A);
        recovery.recordFailure(B, W, H, 0, true);
        recovery.recordFailure(B, W, H, 0, true);
        recovery.recordFailure(B, W, H, 0, true);
        assertEquals(WAIT, evaluate(recovery, PAIRS, NONE, 500));
        recovery.resumeVisuallyReadyPair(B, W, H);
        assertFalse(recovery.isPending());
        assertEquals(Collections.singletonList(B), recovery.availablePairs(PAIRS, W, H));
        assertTrue(recovery.hasFailed(A, W, H));
        assertFalse(recovery.hasFailed(B, W, H));
    }

    @Test
    public void visualReviewWaitIsBoundedAndStartsAfterSettling() {
        AutoPairRecovery recovery = exhausted(A);
        assertFalse(recovery.inVisualReviewWindow(499));
        assertTrue(recovery.inVisualReviewWindow(500));
        assertTrue(recovery.inVisualReviewWindow(1249));
        assertFalse(recovery.inVisualReviewWindow(1250));
        recovery.resumeVisuallyReadyPair(A, W, H);
        assertFalse(recovery.inVisualReviewWindow(700));
    }

    private static AutoPairRecovery.Action evaluate(AutoPairRecovery recovery,
                    List<MatchPair> pairs, List<WordBox> selected, long now) {
        return recovery.evaluate(pairs, selected, true, true, W, H, now);
    }

    private static AutoPairRecovery exhausted(MatchPair pair) {
        AutoPairRecovery recovery = new AutoPairRecovery();
        for (int attempt = 0; attempt < 3; attempt++) {
            recovery.recordFailure(pair, W, H, 0);
        }
        return recovery;
    }

    private static MatchPair pair(String left, String right, int y) {
        return new MatchPair(new WordBox(left, 100, y, 220, y + 50),
                new WordBox(right, 700, y, 820, y + 50), 1.0, 1);
    }
}
