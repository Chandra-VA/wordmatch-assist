package com.wordmatch.assist.matching;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class IssuedPairRetentionPolicyTest {
    @Test
    public void confirmedGreenCardsRemainSuppressedPastAnimationTimeout() {
        assertFalse(IssuedPairRetentionPolicy.shouldRemove(8000L, 4000L, 10, true, true));
        assertFalse(IssuedPairRetentionPolicy.shouldRemove(8000L, 4000L, 9, false, true));
        assertTrue(IssuedPairRetentionPolicy.shouldRemove(8000L, 4000L, 8, false, false));
    }

    @Test
    public void keepsIssuedPairAcrossEmptyPauseFrames() {
        assertFalse(IssuedPairRetentionPolicy.shouldRemove(
                300L,
                4000L,
                0,
                false,
                false
        ));
    }

    @Test
    public void removesIssuedPairAfterAReadableBoardReplacesBothTargets() {
        assertTrue(IssuedPairRetentionPolicy.shouldRemove(
                300L,
                4000L,
                8,
                false,
                false
        ));
    }

    @Test
    public void expiresStaleRecordEvenWithoutAReadableBoard() {
        assertTrue(IssuedPairRetentionPolicy.shouldRemove(
                4001L,
                4000L,
                0,
                false,
                false
        ));
    }
}
