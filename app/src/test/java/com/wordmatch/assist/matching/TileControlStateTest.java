package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class TileControlStateTest {
    private static final WordBox WORD = new WordBox("有点", 150, 520, 210, 550);

    @Test public void nonClickableDisabledGreyCardsAreNotEligibleButLongTextLeavesStayUnknown() {
        TileControlState state = new TileControlState();
        state.observe(false, false, true, 20, 500, 480, 580, 1000, 1600);
        assertTrue(state.isInactive(WORD));
        assertFalse(state.hasActiveControl(WORD));
        TileControlState textOnly = new TileControlState();
        textOnly.observe(false, false, true, 50, 520, 450, 550, 1000, 1600);
        assertFalse(textOnly.isInactive(WORD));
    }

    @Test public void successfulNonClickableSlotRetainsGeometryWithoutAuthorizingTouches() {
        TileControlState state = new TileControlState();
        state.observe(false, false, true, 20, 500, 480, 580, 1000, 1600);
        assertEquals(new WordBox("", 20, 500, 480, 580), state.cardBounds(WORD, 1000));
        assertFalse(state.hasActiveControl(WORD));
        state.observe(true, true, true, 20, 500, 480, 580, 1000, 1600);
        assertTrue(state.hasActiveControl(WORD));
    }

    @Test public void hiddenOrOversizedContainersCannotValidateAFadedSlot() {
        TileControlState state = new TileControlState();
        state.observe(false, false, false, 20, 500, 480, 580, 1000, 1600);
        state.observe(false, true, true, 0, 400, 1000, 800, 1000, 1600);
        assertNull(state.cardBounds(WORD, 1000));
        assertFalse(state.hasActiveControl(WORD));
    }

    @Test public void visualSamplingUsesWideCardInsteadOfClickableTextLeaf() {
        TileControlState state = new TileControlState();
        state.observe(true, true, true, 150, 520, 210, 550, 1000, 1600);
        assertNull(state.cardBounds(WORD, 1000));
        state.observe(true, true, true, 20, 500, 480, 580, 1000, 1600);
        assertEquals(new WordBox("", 20, 500, 480, 580), state.cardBounds(WORD, 1000));
    }

    @Test public void disabledCardStillProvidesGeometryButOtherRowsDoNot() {
        TileControlState state = new TileControlState();
        state.observe(true, false, true, 20, 500, 480, 580, 1000, 1600);
        assertEquals(new WordBox("", 20, 500, 480, 580), state.cardBounds(WORD, 1000));
        assertNull(state.cardBounds(new WordBox("other", 150, 700, 210, 730), 1000));
    }

    @Test
    public void unknownControlsRemainEligible() {
        assertFalse(new TileControlState().isInactive(WORD));
    }

    @Test
    public void disabledTextLeavesDoNotHideAnActiveCard() {
        TileControlState state = new TileControlState();
        state.observe(false, false, false, 150, 520, 210, 550, 800, 1280);
        assertFalse(state.isInactive(WORD));
        state.observe(true, true, true, 20, 500, 380, 565, 800, 1280);
        assertFalse(state.isInactive(WORD));
    }

    @Test
    public void disabledCardControlIsIneligibleWithoutRemovingOtherCards() {
        TileControlState state = new TileControlState();
        state.observe(true, false, true, 20, 500, 380, 565, 800, 1280);
        assertTrue(state.isInactive(WORD));
        assertFalse(state.isInactive(new WordBox("吐司", 150, 620, 210, 650)));
        assertFalse(state.isInactive(new WordBox("toast", 550, 520, 610, 550)));
    }

    @Test
    public void activeParentOverridesDisabledChildControl() {
        TileControlState state = new TileControlState();
        state.observe(true, false, false, 150, 520, 210, 550, 800, 1280);
        state.observe(true, true, true, 20, 500, 380, 565, 800, 1280);
        assertFalse(state.isInactive(WORD));
    }

    @Test
    public void broadScreenOrDialogContainerCannotDisableAllWords() {
        TileControlState state = new TileControlState();
        state.observe(true, false, false, 0, 0, 800, 1280, 800, 1280);
        state.observe(true, false, false, 0, 400, 800, 600, 800, 1280);
        assertFalse(state.isInactive(WORD));
    }
}
