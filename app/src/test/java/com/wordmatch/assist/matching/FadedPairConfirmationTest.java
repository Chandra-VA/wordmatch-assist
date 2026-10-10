package com.wordmatch.assist.matching;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.wordmatch.assist.matching.CardVisualState.State.*;

public final class FadedPairConfirmationTest {
    private boolean good(FadedPairConfirmation confirmation, long now) {
        return confirmation.observe(FADED, FADED, true, false, false, true, true, 1000, now);
    }

    @Test public void missedGreenCanFinishWithTwoStableFadedAndDisabledObservations() {
        FadedPairConfirmation confirmation = new FadedPairConfirmation();
        assertFalse(good(confirmation, 1160));
        assertFalse(good(confirmation, 1255));
        assertTrue(good(confirmation, 1256));
    }

    @Test public void slowDeviceScreenshotIntervalStillConfirmsWithoutWaitingForTimeout() {
        FadedPairConfirmation confirmation = new FadedPairConfirmation();
        assertFalse(good(confirmation, 1160));
        assertTrue(good(confirmation, 2260));
    }

    @Test public void allCorroboratingConditionsAreRequiredAndLosingAnyResetsEvidence() {
        for (int absent = 0; absent < 7; absent++) {
            FadedPairConfirmation confirmation = new FadedPairConfirmation();
            assertFalse(good(confirmation, 1160));
            assertFalse(confirmation.observe(absent == 0 ? READY : FADED, absent == 1 ? UNKNOWN : FADED,
                    absent != 2, absent == 3, absent == 4, absent != 5, absent != 6, 1000, 1300));
            assertFalse(good(confirmation, 1400));
            assertTrue(good(confirmation, 1496));
        }
    }

    @Test public void blankGreenSelectedOrUnknownPartnerDoesNotUseFadedConfirmation() {
        for (CardVisualState.State state : CardVisualState.State.values()) {
            if (state == FADED) continue;
            FadedPairConfirmation confirmation = new FadedPairConfirmation();
            assertFalse(confirmation.observe(FADED, state, true, false, false, true, true, 1000, 1160));
            assertFalse(confirmation.observe(FADED, state, true, false, false, true, true, 1000, 1500));
        }
    }

    @Test public void clickAgeClockRollbackLongGapAndNewPairCannotReuseEvidence() {
        FadedPairConfirmation confirmation = new FadedPairConfirmation();
        assertFalse(good(confirmation, 999));
        assertFalse(good(confirmation, 1159));
        assertFalse(good(confirmation, 1160));
        assertFalse(good(confirmation, 3000));
        assertFalse(good(confirmation, 2800));
        confirmation.reset();
        assertFalse(good(confirmation, 3100));
        assertTrue(good(confirmation, 3196));
    }
}
