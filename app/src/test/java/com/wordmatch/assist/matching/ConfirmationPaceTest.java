package com.wordmatch.assist.matching;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ConfirmationPaceTest {
    @Test public void measuresConfirmedPairsNotNodeReadsAndIncludesPausedTime() {
        ConfirmationPace pace = new ConfirmationPace();
        pace.start(1000);
        assertTrue(Double.isNaN(pace.perSecond(1999)));
        for (int i = 0; i < 9; i++) pace.confirmed(1100 + i * 300L);
        assertEquals(3.0, pace.perSecond(4000), 0.001);
        assertEquals(1.0, pace.perSecond(5800), 0.001);
        assertEquals(0.0, pace.perSecond(8000), 0.001);
    }
    @Test public void startupAndNewSessionDoNotDisplayInflatedInstantaneousRates() {
        ConfirmationPace pace = new ConfirmationPace();
        pace.start(1000);
        pace.confirmed(1100);
        assertTrue(Double.isNaN(pace.perSecond(1100)));
        assertEquals(1.0, pace.perSecond(2000), 0.001);
        pace.reset();
        assertTrue(Double.isNaN(pace.perSecond(3000)));
    }
}
