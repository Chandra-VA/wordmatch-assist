package com.wordmatch.assist.matching;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.wordmatch.assist.matching.CardVisualState.State.*;
import static com.wordmatch.assist.matching.PartialPairRetry.Action.*;

public final class PartialPairRetryTest {
    @Test public void selectedLectureRetriesOnlyTheUntouchedPartnerBeforeSixSecondTimeout() {
        PartialPairRetry retry = new PartialPairRetry();
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 1100));
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 1160));
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 1207));
        assertEquals(SECOND, retry.observe(SELECTED, READY, 1000, 1208));
    }

    @Test public void onlySecondSelectedRetriesFirstInsteadOfDeselectingSecond() {
        PartialPairRetry retry = new PartialPairRetry();
        assertEquals(NONE, retry.observe(READY, SELECTED, 1000, 1200));
        assertEquals(FIRST, retry.observe(READY, SELECTED, 1000, 1248));
    }

    @Test public void missingSelectionNeverGuessesAndTwoUntouchedCardsRequireLongerReview() {
        PartialPairRetry retry = new PartialPairRetry();
        assertEquals(NONE, retry.observe(READY, READY, 1000, 1300));
        assertEquals(NONE, retry.observe(READY, READY, 1000, 1450));
        assertEquals(RECHECK, retry.observe(READY, READY, 1000, 1498));
    }

    @Test public void retriesAreBoundedAndCannotFireBeforePreviousTapSettles() {
        PartialPairRetry retry = new PartialPairRetry();
        retry.observe(SELECTED, READY, 1000, 1200);
        assertEquals(SECOND, retry.observe(SELECTED, READY, 1000, 1248));
        retry.recordRetry(1248);
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 1300));
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 1408));
        assertEquals(SECOND, retry.observe(SELECTED, READY, 1000, 1456));
        retry.recordRetry(1456);
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 1800));
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 2000));
        retry.reset();
        retry.observe(SELECTED, READY, 2000, 2200);
        assertEquals(SECOND, retry.observe(SELECTED, READY, 2000, 2248));
    }

    @Test public void successUnknownMultipleSelectionAndFrameGapsInvalidateOldEvidence() {
        PartialPairRetry retry = new PartialPairRetry();
        retry.observe(SELECTED, READY, 1000, 1200);
        assertEquals(NONE, retry.observe(GREEN, GREEN, 1000, 1250));
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 1266));
        assertEquals(NONE, retry.observe(SELECTED, UNKNOWN, 1000, 1320));
        assertEquals(NONE, retry.observe(SELECTED, SELECTED, 1000, 1400));
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 1450));
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 2100));
        assertEquals(NONE, retry.observe(SELECTED, READY, 1000, 2050));
        assertEquals(SECOND, retry.observe(SELECTED, READY, 1000, 2098));
    }
}
