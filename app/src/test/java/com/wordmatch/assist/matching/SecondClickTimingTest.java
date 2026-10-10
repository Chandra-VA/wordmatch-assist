package com.wordmatch.assist.matching;

import org.junit.Test;
import static org.junit.Assert.*;

public final class SecondClickTimingTest {
    @Test public void observedSelectionCanFinishPairOnFirstPoll() {
        assertTrue(SecondClickTiming.isReady(true, 16));
        assertFalse(SecondClickTiming.isReady(false, 16));
    }

    @Test public void absentSelectionWaitsTwoFramesBeforeFallback() {
        assertFalse(SecondClickTiming.isReady(false, 16));
        assertFalse(SecondClickTiming.isReady(false, 31));
        assertTrue(SecondClickTiming.isReady(false, 32));
        assertTrue(SecondClickTiming.isReady(false, 100));
        assertFalse(SecondClickTiming.isReady(true, -1));
    }
}
