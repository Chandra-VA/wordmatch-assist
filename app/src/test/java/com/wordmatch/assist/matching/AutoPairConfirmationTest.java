package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AutoPairConfirmationTest {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1600;
    private static final WordBox LEFT = box("地毯", 100, 300);
    private static final WordBox RIGHT = box("carpet", 700, 300);
    private static final WordBox OTHER_LEFT = box("早晨", 100, 450);
    private static final WordBox OTHER_RIGHT = box("morning", 700, 450);
    private static final List<WordBox> OTHER_PAIR = Arrays.asList(OTHER_LEFT, OTHER_RIGHT);

    @Test public void slowdownPausePreservesTargetsAndCounterButRequiresFreshPostResumeEvidence() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        confirmation.start(LEFT, RIGHT, 4, WIDTH, HEIGHT, 5);
        List<WordBox> board = Arrays.asList(LEFT, RIGHT, OTHER_LEFT, OTHER_RIGHT);
        assertFalse(confirmation.observe(board, true, 6, 1000));
        // Opening a pause clears transient observations, without canceling the target pair.
        assertFalse(confirmation.observe(null, false, 1100));
        assertFalse(confirmation.observe(Collections.emptyList(), false, 6, 1800));
        assertFalse(confirmation.observe(board, true, 6, 2000));
        assertFalse(confirmation.observe(board, true, 6, 2015));
        assertTrue(confirmation.observe(board, true, 6, 2016));
    }

    @Test public void resumedUnchangedPairCannotBeAssumedCompleteJustBecausePauseEnded() {
        AutoPairConfirmation confirmation = started(4);
        List<WordBox> board = Arrays.asList(LEFT, RIGHT, OTHER_LEFT, OTHER_RIGHT);
        assertFalse(confirmation.observe(board, true, 1000));
        assertFalse(confirmation.observe(null, false, 1100));
        assertFalse(confirmation.observe(board, true, 2000));
        assertFalse(confirmation.observe(board, true, 2200));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 2300));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 2380));
    }

    @Test
    public void neverConfirmsWhileBothClickedCardsRemainOnTheBoard() {
        AutoPairConfirmation confirmation = started(4);
        List<WordBox> unchanged = Arrays.asList(LEFT, RIGHT, OTHER_LEFT, OTHER_RIGHT);
        for (long now = 0L; now <= 10_000L; now += 100L) {
            assertFalse(confirmation.observe(unchanged, true, now));
        }
    }

    @Test
    public void doesNotConfirmWhileEitherClickedCardRemains() {
        AutoPairConfirmation confirmation = started(4);
        List<WordBox> rightSelected = Arrays.asList(RIGHT, OTHER_LEFT, OTHER_RIGHT);
        List<WordBox> leftRemaining = Arrays.asList(LEFT, OTHER_LEFT, OTHER_RIGHT);
        assertFalse(confirmation.observe(rightSelected, true, 100L));
        assertFalse(confirmation.observe(rightSelected, true, 400L));
        assertFalse(confirmation.observe(leftRemaining, true, 500L));
        assertFalse(confirmation.observe(leftRemaining, true, 800L));
    }

    @Test
    public void requiresEightyMillisecondsAndAnotherObservationForFinalPair() {
        AutoPairConfirmation confirmation = started(2);
        List<WordBox> empty = Collections.emptyList();
        assertFalse(confirmation.observe(empty, true, 0L));
        assertFalse(confirmation.observe(empty, true, 0L));
        assertFalse(confirmation.observe(empty, true, 79L));
        assertTrue(confirmation.observe(empty, true, 80L));
    }

    @Test
    public void incompleteScanBreaksConfirmationWithoutForgettingTargets() {
        AutoPairConfirmation confirmation = started(4);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 100L));
        assertFalse(confirmation.observe(OTHER_PAIR, false, 180L));
        assertFalse(confirmation.observe(OTHER_PAIR, false, 280L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 300L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 379L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 380L));
    }

    @Test
    public void temporaryEmptyBoardDoesNotConfirmANonFinalPair() {
        AutoPairConfirmation confirmation = started(4);
        List<WordBox> empty = Collections.emptyList();
        assertFalse(confirmation.observe(OTHER_PAIR, true, 0L));
        assertFalse(confirmation.observe(empty, true, 80L));
        assertFalse(confirmation.observe(empty, true, 200L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 300L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 380L));
    }

    @Test
    public void reappearingTargetRestartsStableDisappearance() {
        AutoPairConfirmation confirmation = started(4);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 100L));
        assertFalse(confirmation.observe(
                Arrays.asList(RIGHT, OTHER_LEFT, OTHER_RIGHT), true, 180L
        ));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 200L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 279L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 280L));
    }

    @Test
    public void longObservationGapRestartsStableDisappearance() {
        AutoPairConfirmation confirmation = started(4);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 1000L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 1501L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 1580L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 1581L));
    }

    @Test
    public void fiveHundredMillisecondGapIsStillContinuous() {
        AutoPairConfirmation confirmation = started(4);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 1000L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 1500L));
    }

    @Test
    public void clockRollbackRestartsStableDisappearance() {
        AutoPairConfirmation confirmation = started(4);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 1000L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 900L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 979L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 980L));
    }

    @Test
    public void inactiveAndResetConfirmationsCannotSucceed() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        assertFalse(confirmation.observe(OTHER_PAIR, true, 0L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 100L));
        confirmation.start(LEFT, RIGHT, 4, WIDTH, HEIGHT);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 200L));
        confirmation.reset();
        assertFalse(confirmation.observe(OTHER_PAIR, true, 300L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 400L));
    }

    @Test
    public void nullObservationRestartsStableDisappearance() {
        AutoPairConfirmation confirmation = started(4);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 100L));
        assertFalse(confirmation.observe(null, true, 180L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 200L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 280L));
    }

    @Test
    public void eachPairUsesItsOwnFreshBoardSizeThroughTheWholeTenCardBoard() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        List<WordBox> board = new ArrayList<>();
        for (int pair = 0; pair < 5; pair++) {
            board.add(box("left" + pair, 100, 300 + pair * 120));
            board.add(box("right" + pair, 700, 300 + pair * 120));
        }
        long now = 0L;
        while (!board.isEmpty()) {
            WordBox first = board.get(0);
            WordBox second = board.get(1);
            confirmation.start(first, second, board.size(), WIDTH, HEIGHT);
            assertFalse(confirmation.observe(board, true, now));
            board = new ArrayList<>(board.subList(2, board.size()));
            assertFalse(confirmation.observe(board, true, now + 10L));
            assertFalse(confirmation.observe(board, true, now + 89L));
            assertTrue(confirmation.observe(board, true, now + 90L));
            now += 100L;
        }
    }

    @Test
    public void restartingTheSameTargetsRequiresNewConfirmation() {
        AutoPairConfirmation confirmation = started(4);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 100L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 180L));
        confirmation.start(LEFT, RIGHT, 4, WIDTH, HEIGHT);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 200L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 279L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 280L));
    }

    private static AutoPairConfirmation started(int baselineWordCount) {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        confirmation.start(LEFT, RIGHT, baselineWordCount, WIDTH, HEIGHT);
        return confirmation;
    }

    @Test
    public void recordedGreenCardsCanRemainVisibleAfterStreakIncreases() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        List<WordBox> greenCardsStillPresent = Arrays.asList(LEFT, RIGHT, OTHER_LEFT, OTHER_RIGHT);
        confirmation.start(LEFT, RIGHT, 4, WIDTH, HEIGHT, 2);
        assertFalse(confirmation.observe(greenCardsStillPresent, true, 2, 0L));
        assertFalse(confirmation.observe(greenCardsStillPresent, true, 3, 100L));
        assertFalse(confirmation.observe(greenCardsStillPresent, true, 3, 115L));
        assertTrue(confirmation.observe(greenCardsStillPresent, true, 3, 116L));
    }

    @Test
    public void unchangedOrDecreasedStreakIsNotSuccess() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        List<WordBox> board = Arrays.asList(LEFT, RIGHT);
        confirmation.start(LEFT, RIGHT, 2, WIDTH, HEIGHT, 2);
        assertFalse(confirmation.observe(board, true, 2, 0L));
        assertFalse(confirmation.observe(board, true, 2, 100L));
        assertFalse(confirmation.observe(board, true, 0, 200L));
        assertFalse(confirmation.observe(board, true, 0, 300L));
    }

    @Test
    public void unknownBaselineDoesNotTreatAnExistingStreakAsNewSuccess() {
        AutoPairConfirmation confirmation = started(2);
        assertFalse(confirmation.observe(Arrays.asList(LEFT, RIGHT), true, 3, 0L));
        assertFalse(confirmation.observe(Arrays.asList(LEFT, RIGHT), true, 3, 100L));
    }

    @Test
    public void nextPairCannotReusePreviousPairsSuccessCounter() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        List<WordBox> board = Arrays.asList(LEFT, RIGHT, OTHER_LEFT, OTHER_RIGHT);
        confirmation.start(LEFT, RIGHT, 4, WIDTH, HEIGHT, 2);
        assertFalse(confirmation.observe(board, true, 3, 0L));
        assertTrue(confirmation.observe(board, true, 3, 80L));
        confirmation.start(OTHER_LEFT, OTHER_RIGHT, 4, WIDTH, HEIGHT, 3);
        assertFalse(confirmation.observe(board, true, 3, 100L));
        assertFalse(confirmation.observe(board, true, 3, 200L));
        assertFalse(confirmation.observe(board, true, 4, 300L));
        assertTrue(confirmation.observe(board, true, 4, 380L));
    }

    @Test
    public void incompleteOrModalFrameCannotConfirmCounterIncrease() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        List<WordBox> board = Arrays.asList(LEFT, RIGHT);
        confirmation.start(LEFT, RIGHT, 2, WIDTH, HEIGHT, 2);
        assertFalse(confirmation.observe(board, true, 3, 0L));
        assertFalse(confirmation.observe(board, false, 3, 80L));
        assertFalse(confirmation.observe(board, true, 3, 100L));
        assertTrue(confirmation.observe(board, true, 3, 180L));
    }

    @Test
    public void transientCounterIncreaseMustNotTriggerTheNextPair() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        List<WordBox> board = Arrays.asList(LEFT, RIGHT);
        confirmation.start(LEFT, RIGHT, 2, WIDTH, HEIGHT, 2);
        assertFalse(confirmation.observe(board, true, 3, 0L));
        assertFalse(confirmation.observe(board, true, 2, 80L));
        assertFalse(confirmation.observe(board, true, 3, 100L));
        assertFalse(confirmation.observe(board, true, -1, 180L));
        assertFalse(confirmation.observe(board, true, 3, 200L));
        assertTrue(confirmation.observe(board, true, 3, 280L));
    }

    @Test
    public void feedbackAfterFirstClickAvoidsAClickOnAlreadyCompletedPartner() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        List<WordBox> board = Arrays.asList(LEFT, RIGHT);
        confirmation.start(LEFT, RIGHT, 2, WIDTH, HEIGHT, 2);
        assertFalse(confirmation.hasSuccessEvidence(board, 2));
        assertTrue(confirmation.hasSuccessEvidence(board, 3));
        assertFalse(confirmation.observe(board, true, 3, 0L));
        assertTrue(confirmation.observe(board, true, 3, 80L));
    }

    @Test
    public void slowAnimationWithoutCounterStillConfirmsOnDisappearance() {
        AutoPairConfirmation confirmation = started(2);
        List<WordBox> board = Arrays.asList(LEFT, RIGHT);
        for (long now = 0L; now < 4000L; now += 100L) {
            assertFalse(confirmation.observe(board, true, now));
        }
        assertFalse(confirmation.observe(Collections.emptyList(), true, 4000L));
        assertTrue(confirmation.observe(Collections.emptyList(), true, 4080L));
    }

    @Test
    public void overlappingCompletedCardsMustNotInflateTheNextPairsBaseline() {
        // Six old green cards can still be rendered, but only these four cards
        // remain unresolved. All old cards may disappear together with this pair.
        List<WordBox> unresolved = Arrays.asList(LEFT, RIGHT, OTHER_LEFT, OTHER_RIGHT);
        AutoPairConfirmation confirmation = started(unresolved.size());
        assertFalse(confirmation.observe(unresolved, true, 0L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, 100L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, 180L));

        AutoPairConfirmation inflatedBaseline = started(unresolved.size() + 6);
        assertFalse(inflatedBaseline.observe(OTHER_PAIR, true, 100L));
        assertFalse(inflatedBaseline.observe(OTHER_PAIR, true, 180L));
    }

    private static WordBox box(String text, int x, int y) {
        return new WordBox(text, x, y, x + 120, y + 50);
    }

    @Test
    public void progressConfirmsWhileGreenCardsRemainWithoutStreakFeedback() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        List<WordBox> board = Arrays.asList(LEFT, RIGHT, OTHER_LEFT, OTHER_RIGHT);
        confirmation.start(LEFT, RIGHT, 4, WIDTH, HEIGHT, -1,
                MatchProgressReaderTest.reading(5, 30));
        assertFalse(confirmation.observe(board, true, -1, MatchProgressReaderTest.reading(5, 30), 0));
        assertFalse(confirmation.observe(board, true, -1, MatchProgressReaderTest.reading(6, 30), 16));
        assertTrue(confirmation.observe(board, true, -1, MatchProgressReaderTest.reading(6, 30), 32));
        confirmation.start(OTHER_LEFT, OTHER_RIGHT, 2, WIDTH, HEIGHT, -1,
                MatchProgressReaderTest.reading(6, 30));
        assertFalse(confirmation.observe(board, true, -1, MatchProgressReaderTest.reading(6, 30), 48));
        assertFalse(confirmation.observe(board, true, -1, MatchProgressReaderTest.reading(6, 30), 80));
    }

    @Test
    public void progressCannotBypassMissingBaselineModalOrIncompleteScan() {
        List<WordBox> board = Arrays.asList(LEFT, RIGHT);
        AutoPairConfirmation confirmation = started(2);
        assertFalse(confirmation.observe(board, true, -1, MatchProgressReaderTest.reading(6, 30), 0));
        assertFalse(confirmation.observe(board, true, -1, MatchProgressReaderTest.reading(6, 30), 32));
        confirmation.start(LEFT, RIGHT, 2, WIDTH, HEIGHT, -1, MatchProgressReaderTest.reading(5, 30));
        assertFalse(confirmation.observe(board, false, -1, MatchProgressReaderTest.reading(6, 30), 64));
        assertFalse(confirmation.observe(board, false, -1, MatchProgressReaderTest.reading(6, 30), 96));
        assertFalse(confirmation.observe(board, true, -1, MatchProgressReaderTest.reading(6, 40), 128));
    }

    @Test
    public void disappearingCounterDoesNotReuseItsShortConfirmationWindow() {
        AutoPairConfirmation confirmation = new AutoPairConfirmation();
        confirmation.start(LEFT, RIGHT, 4, WIDTH, HEIGHT, 2);
        assertFalse(confirmation.observe(OTHER_PAIR, true, 3, 0L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, -1, 16L));
        assertFalse(confirmation.observe(OTHER_PAIR, true, -1, 95L));
        assertTrue(confirmation.observe(OTHER_PAIR, true, -1, 96L));
    }
}
