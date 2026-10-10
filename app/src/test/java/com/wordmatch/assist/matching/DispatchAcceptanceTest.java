package com.wordmatch.assist.matching;

import org.junit.Test;
import static org.junit.Assert.*;

public final class DispatchAcceptanceTest {
    @Test public void anotherPairCanStartAfterStableDisabledTransitionWithoutWaitingForFade() {
        DispatchAcceptance gate = new DispatchAcceptance();
        assertFalse(gate.observe(true, true, true, true, true, 1000, 1032));
        assertFalse(gate.observe(true, true, true, true, true, 1000, 1047));
        assertTrue(gate.observe(true, true, true, true, true, 1000, 1048));
    }
    @Test public void missingAnyEvidenceBreaksTheDispatchWindow() {
        for (int absent = 0; absent < 5; absent++) {
            DispatchAcceptance gate = new DispatchAcceptance();
            assertFalse(gate.observe(true, true, true, true, true, 1000, 1032));
            assertFalse(gate.observe(absent != 0, absent != 1, absent != 2, absent != 3, absent != 4, 1000, 1050));
            assertFalse(gate.observe(true, true, true, true, true, 1000, 1060));
            assertTrue(gate.observe(true, true, true, true, true, 1000, 1076));
        }
    }
    @Test public void lastPairOrResidualSelectionCannotBeReleasedAsIfSuccessful() {
        DispatchAcceptance gate = new DispatchAcceptance();
        assertFalse(gate.observe(true, true, true, true, false, 1000, 1200));
        assertFalse(gate.observe(true, true, true, true, false, 1000, 1600));
        assertFalse(gate.observe(true, true, false, true, true, 1000, 1800));
    }
    @Test public void freshClickClockRollbackGapAndNewPairRequireNewObservations() {
        DispatchAcceptance gate = new DispatchAcceptance();
        assertFalse(gate.observe(true, true, true, true, true, 1000, 1031));
        assertFalse(gate.observe(true, true, true, true, true, 1000, 1050));
        assertFalse(gate.observe(true, true, true, true, true, 1000, 1040));
        assertFalse(gate.observe(true, true, true, true, true, 1000, 1500));
        gate.reset();
        assertFalse(gate.observe(true, true, true, true, true, 1000, 1520));
    }
}
