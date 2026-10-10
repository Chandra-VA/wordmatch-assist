package com.wordmatch.assist.matching;

/** Allows another independent pair to be sent, without claiming the old pair succeeded. */
public final class DispatchAcceptance {
    private long since = -1, last = -1;
    public boolean observe(boolean startedActive, boolean bothInactive, boolean completeUnselectedBoard,
                           boolean sameTargets, boolean anotherReadyPair, long clickedAt, long now) {
        if (!startedActive || !bothInactive || !completeUnselectedBoard || !sameTargets
                || !anotherReadyPair || clickedAt <= 0L || now - clickedAt < 32L) {
            reset(); return false;
        }
        if (since < 0L || now < last || now - last > 200L) since = now;
        last = now;
        return now - since >= 16L;
    }
    public void reset() { since = last = -1L; }
}
