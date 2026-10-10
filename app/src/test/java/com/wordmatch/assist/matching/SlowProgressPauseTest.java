package com.wordmatch.assist.matching;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.wordmatch.assist.matching.SlowProgressPause.Action.*;

public final class SlowProgressPauseTest {
    @Test public void pausesAnUnconfirmedPairWellBeforeTheSixSecondTimeout() {
        SlowProgressPause policy = new SlowProgressPause();
        policy.start(1000);
        assertEquals(NONE, policy.action(1479));
        assertEquals(RECOVER, policy.action(1480));
    }

    @Test public void scansClicksAndAnimationsDoNotPretendToBeConfirmedProgress() {
        SlowProgressPause policy = new SlowProgressPause();
        policy.start(1000);
        policy.start(1200);
        policy.start(1400);
        assertEquals(RECOVER, policy.action(1480));
    }

    @Test public void fastConfirmedPairsRemainUninterruptedAndDetectLaterSlowdownEarlier() {
        SlowProgressPause policy = new SlowProgressPause();
        policy.start(1000);
        for (long now = 1100; now <= 2000; now += 100) {
            assertEquals(NONE, policy.action(now));
            policy.completed(now, 100);
        }
        assertEquals(320, policy.thresholdMs());
        assertEquals(NONE, policy.action(2319));
        assertEquals(RECOVER, policy.action(2320));
    }

    @Test public void slowerSuccessfulDeviceHasABoundedGracePeriod() {
        SlowProgressPause policy = new SlowProgressPause();
        policy.completed(1000, 240);
        assertEquals(600, policy.thresholdMs());
        for (int i = 0; i < 10; i++) policy.completed(2000 + i * 1000L, 1000);
        assertEquals(800, policy.thresholdMs());
        assertEquals(NONE, policy.action(11799));
        assertEquals(RECOVER, policy.action(11800));
    }

    @Test public void twoUnsuccessfulRecoveryPausesThenHoldWithoutInfiniteResumeLoops() {
        SlowProgressPause policy = new SlowProgressPause();
        policy.start(1000);
        assertEquals(RECOVER, policy.action(1480));
        policy.requested();
        assertEquals(NONE, policy.action(9000));
        policy.resumed(10000);
        assertEquals(NONE, policy.action(10479));
        assertEquals(RECOVER, policy.action(10480));
        policy.requested();
        policy.resumed(11000);
        assertEquals(HOLD, policy.action(11480));
        policy.requested();
        assertEquals(NONE, policy.action(999999));
    }

    @Test public void pausedPairDoesNotInflatePaceAndOnlySuccessReplenishesRecovery() {
        SlowProgressPause policy = new SlowProgressPause();
        policy.completed(1000, 100);
        policy.requested();
        policy.resumed(10000);
        policy.requested();
        policy.resumed(11000);
        assertEquals(HOLD, policy.action(11320));
        policy.completed(11400, 0);
        assertEquals(320, policy.thresholdMs());
        assertEquals(RECOVER, policy.action(11720));
    }

    @Test public void failedNavigationBacksOffAndUnverifiedPauseCannotLoop() {
        SlowProgressPause policy = new SlowProgressPause();
        policy.start(1000);
        policy.retryLater(1480);
        assertEquals(NONE, policy.action(2479));
        assertEquals(RECOVER, policy.action(2480));
        policy.requested();
        policy.openingFailed();
        policy.resumed(3000);
        assertEquals(NONE, policy.action(9000));
        policy.completed(9100, 100);
        assertEquals(RECOVER, policy.action(9420));
    }

    @Test public void disabledSessionAndClockRollbackCannotRequestPause() {
        SlowProgressPause policy = new SlowProgressPause();
        assertEquals(NONE, policy.action(999999));
        policy.start(1000);
        assertEquals(NONE, policy.action(999));
        policy.reset();
        assertEquals(NONE, policy.action(999999));
        policy.start(1000000);
        assertEquals(NONE, policy.action(1000479));
        assertEquals(RECOVER, policy.action(1000480));
    }
}
