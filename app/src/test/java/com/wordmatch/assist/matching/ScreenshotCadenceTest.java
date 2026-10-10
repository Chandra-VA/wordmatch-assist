package com.wordmatch.assist.matching;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ScreenshotCadenceTest {
    @Test public void consecutivePairsShareThePlatformCaptureBudget() {
        ScreenshotCadence cadence = new ScreenshotCadence();
        assertTrue(cadence.canRequest(1000));
        cadence.requested(1000);
        assertFalse(cadence.canRequest(1349));
        assertTrue(cadence.canRequest(1350));
        cadence.requested(1350);
        assertFalse(cadence.canRequest(1699));
        assertTrue(cadence.canRequest(1700));
    }

    @Test public void throttledDeviceBacksOffToBoundedCompatibleInterval() {
        ScreenshotCadence cadence = new ScreenshotCadence();
        cadence.requested(1000);
        long time = 1010;
        for (long wait : new long[]{600, 850, 1100, 1100}) {
            cadence.throttled(time);
            assertFalse(cadence.canRequest(time + wait - 1));
            assertTrue(cadence.canRequest(time + wait));
            cadence.requested(time + wait);
            time += wait + 10;
        }
    }
}
