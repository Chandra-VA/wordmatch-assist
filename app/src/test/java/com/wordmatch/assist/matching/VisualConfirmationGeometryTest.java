package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class VisualConfirmationGeometryTest {
    private static final WordBox CARD = new WordBox("", 16, 558, 389, 623);
    private static final WordBox SPILL = new WordBox("溢出", 185, 580, 221, 600);
    private static final WordBox ELSEWHERE = new WordBox("airline", 569, 820, 630, 850);

    @Test public void completedTextCanFadeWhileItsGreenCardSlotStaysPresent() {
        assertTrue(VisualConfirmationGeometry.matches(Arrays.asList(SPILL, ELSEWHERE), SPILL, CARD, CARD));
        assertTrue(VisualConfirmationGeometry.matches(Collections.singletonList(ELSEWHERE), SPILL, CARD, CARD));
        assertTrue(VisualConfirmationGeometry.matches(Collections.emptyList(), SPILL, CARD, CARD));
    }

    @Test public void missingTextAndMissingCardAreInsufficientEvenWithUnrelatedWords() {
        assertFalse(VisualConfirmationGeometry.matches(Collections.singletonList(ELSEWHERE), SPILL, CARD, null));
        assertTrue(VisualConfirmationGeometry.matches(Collections.singletonList(SPILL), SPILL, CARD, null));
    }

    @Test public void replacementTextMovedLabelAndMovedCardInvalidateCapturedPixels() {
        WordBox replacement = new WordBox("新词", 185, 580, 221, 600);
        WordBox moved = new WordBox("溢出", 195, 580, 231, 600);
        WordBox shiftedCard = new WordBox("", 16, 563, 389, 628);
        assertFalse(VisualConfirmationGeometry.matches(Collections.singletonList(replacement), SPILL, CARD, CARD));
        assertFalse(VisualConfirmationGeometry.matches(Collections.singletonList(moved), SPILL, CARD, CARD));
        assertFalse(VisualConfirmationGeometry.matches(Collections.singletonList(SPILL), SPILL, CARD, shiftedCard));
        assertFalse(VisualConfirmationGeometry.matches(null, SPILL, CARD, CARD));
    }

    @Test public void smallDisabledBorderChangeNeedsTheSameTextAnchor() {
        WordBox shiftedCard = new WordBox("", 18, 560, 387, 627);
        assertTrue(VisualConfirmationGeometry.matches(Collections.singletonList(SPILL), SPILL, CARD, shiftedCard));
        assertFalse(VisualConfirmationGeometry.matches(Collections.emptyList(), SPILL, CARD, shiftedCard));
    }
}
