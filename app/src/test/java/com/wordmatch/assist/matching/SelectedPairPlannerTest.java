package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class SelectedPairPlannerTest {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1600;
    private static final WordBox PARTNER_ZH = box("搭档", 100, 300);
    private static final WordBox PARTNER_EN = box("partner", 700, 300);
    private static final WordBox CARPET_ZH = box("地毯", 100, 500);
    private static final WordBox CARPET_EN = box("carpet", 700, 500);
    private static final MatchPair PARTNER = new MatchPair(PARTNER_ZH, PARTNER_EN, 1.0, 1);
    private static final MatchPair CARPET = new MatchPair(CARPET_ZH, CARPET_EN, 1.0, 2);
    private static final List<MatchPair> PAIRS = Arrays.asList(PARTNER, CARPET);

    @Test
    public void selectedCarpetFinishesCarpetBeforeUnrelatedPartnerPair() {
        SelectedPairPlanner.Plan plan = choose(PAIRS, Collections.singletonList(CARPET_EN));

        assertNotNull(plan);
        assertSame(CARPET_EN, plan.getPair().getFirst());
        assertSame(CARPET_ZH, plan.getPair().getSecond());
        assertTrue(plan.isFirstAlreadySelected());
        assertEquals(CARPET.getScore(), plan.getPair().getScore(), 0.0);
        assertEquals(CARPET.getLabel(), plan.getPair().getLabel());
    }

    @Test
    public void selectedFirstTileIsKeptFirstAndMustNotBeClickedAgain() {
        SelectedPairPlanner.Plan plan = choose(PAIRS, Collections.singletonList(CARPET_ZH));

        assertNotNull(plan);
        assertSame(CARPET, plan.getPair());
        assertTrue(plan.isFirstAlreadySelected());
    }

    @Test
    public void multipleSelectionsPreventAnyNewPair() {
        assertNull(choose(PAIRS, Arrays.asList(CARPET_EN, PARTNER_ZH)));
        assertNull(choose(PAIRS, Arrays.asList(CARPET_EN, CARPET_ZH)));
    }

    @Test
    public void unknownSelectedTileDoesNotFallBackToUnrelatedExactPair() {
        assertNull(choose(PAIRS, Collections.singletonList(box("unknown", 700, 500))));
    }

    @Test
    public void selectedTileWithOnlyFuzzyPartnerDoesNotAllowAnotherPair() {
        MatchPair fuzzyCarpet = new MatchPair(CARPET_ZH, CARPET_EN, 0.91, 2);

        assertNull(choose(Arrays.asList(PARTNER, fuzzyCarpet),
                Collections.singletonList(CARPET_EN)));
    }

    @Test
    public void withoutSelectionSkipsFuzzyAndNullPairs() {
        MatchPair fuzzyCarpet = new MatchPair(CARPET_ZH, CARPET_EN, 0.91, 2);
        SelectedPairPlanner.Plan plan = choose(Arrays.asList(null, fuzzyCarpet, PARTNER, CARPET),
                Collections.emptyList());

        assertNotNull(plan);
        assertSame(PARTNER, plan.getPair());
        assertFalse(plan.isFirstAlreadySelected());
        assertNull(choose(Collections.singletonList(fuzzyCarpet), Collections.emptyList()));
    }

    @Test
    public void sameTextAtDifferentPositionIsNotTheSelectedTile() {
        assertNull(choose(PAIRS, Collections.singletonList(box("carpet", 100, 500))));
        assertNull(choose(PAIRS, Collections.singletonList(box("carpet", 700, 700))));
        assertNull(choose(PAIRS, Collections.singletonList(box("carpet", 741, 500))));
        assertNull(choose(PAIRS, Collections.singletonList(box("carpet", 700, 565))));
    }

    @Test
    public void duplicateTextChoosesPairAtTheSelectedPosition() {
        WordBox otherCarpetZh = box("地毯", 100, 800);
        WordBox otherCarpetEn = box("carpet", 700, 800);
        MatchPair otherCarpet = new MatchPair(otherCarpetZh, otherCarpetEn, 1.0, 3);

        SelectedPairPlanner.Plan plan = choose(Arrays.asList(otherCarpet, CARPET),
                Collections.singletonList(CARPET_EN));

        assertNotNull(plan);
        assertSame(CARPET_EN, plan.getPair().getFirst());
        assertSame(CARPET_ZH, plan.getPair().getSecond());
    }

    @Test
    public void normalizedTextAndFourPercentPositionToleranceMatch() {
        SelectedPairPlanner.Plan plan = choose(PAIRS,
                Collections.singletonList(box(" CARPET! ", 740, 564)));

        assertNotNull(plan);
        assertSame(CARPET_EN, plan.getPair().getFirst());
        assertTrue(plan.isFirstAlreadySelected());
    }

    @Test
    public void minimumPositionToleranceIsThirtyTwoPixels() {
        SelectedPairPlanner.Plan plan = SelectedPairPlanner.choose(PAIRS,
                Collections.singletonList(box("carpet", 732, 532)), 500, 500);
        assertNotNull(plan);
        assertTrue(plan.isFirstAlreadySelected());
        assertNull(SelectedPairPlanner.choose(PAIRS,
                Collections.singletonList(box("carpet", 733, 532)), 500, 500));
        assertNull(SelectedPairPlanner.choose(PAIRS,
                Collections.singletonList(box("carpet", 732, 533)), 500, 500));
    }

    @Test
    public void noSelectionChoosesFirstExactPair() {
        SelectedPairPlanner.Plan plan = choose(PAIRS, Collections.emptyList());

        assertNotNull(plan);
        assertSame(PARTNER, plan.getPair());
        assertFalse(plan.isFirstAlreadySelected());
    }

    @Test
    public void nullAndEmptyInputsAreSafe() {
        assertNull(choose(null, null));
        assertNull(choose(null, Collections.singletonList(CARPET_EN)));
        assertNull(choose(Collections.emptyList(), Collections.emptyList()));
        assertNull(choose(Collections.singletonList(null), null));
        assertNull(choose(PAIRS, Collections.singletonList(null)));
        assertNull(choose(PAIRS, Collections.singletonList(box("", 700, 500))));

        SelectedPairPlanner.Plan plan = choose(PAIRS, null);
        assertNotNull(plan);
        assertSame(PARTNER, plan.getPair());
        assertFalse(plan.isFirstAlreadySelected());
    }

    @Test
    public void malformedPairDoesNotCrashOrProduceClicks() {
        MatchPair missingFirst = new MatchPair(null, CARPET_EN, 1.0, 2);
        MatchPair missingSecond = new MatchPair(CARPET_ZH, null, 1.0, 2);
        MatchPair emptyText = new MatchPair(CARPET_ZH, box("", 700, 500), 1.0, 2);

        assertNull(choose(Arrays.asList(missingFirst, missingSecond, emptyText),
                Collections.singletonList(CARPET_EN)));
        SelectedPairPlanner.Plan plan = choose(Arrays.asList(missingFirst, missingSecond, PARTNER),
                Collections.emptyList());
        assertNotNull(plan);
        assertSame(PARTNER, plan.getPair());
    }

    private static SelectedPairPlanner.Plan choose(
            List<MatchPair> pairs,
            List<WordBox> selectedWords
    ) {
        return SelectedPairPlanner.choose(pairs, selectedWords, WIDTH, HEIGHT);
    }

    private static WordBox box(String text, int x, int y) {
        return new WordBox(text, x, y, x + 120, y + 50);
    }
}
