package com.wordmatch.assist.matching;

/** Watches confirmed progress, never treating a click, repaint, or pause as a solved pair. */
public final class SlowProgressPause {
    public enum Action { NONE, RECOVER, HOLD }
    private long progressAt = -1, retryAt;
    private long averagePairMs;
    private int pausesWithoutProgress;
    private boolean paused, blocked;

    public void start(long now) {
        if (progressAt < 0) progressAt = now;
    }

    public long thresholdMs() {
        return averagePairMs == 0 ? 480L : Math.max(320L, Math.min(800L, averagePairMs * 5L / 2L));
    }

    public Action action(long now) {
        if (progressAt < 0 || paused || blocked || now < progressAt || now < retryAt
                || now - progressAt < thresholdMs()) return Action.NONE;
        return pausesWithoutProgress < 2 ? Action.RECOVER : Action.HOLD;
    }

    public void requested() { paused = true; pausesWithoutProgress++; }

    /** Resume grants a fresh observation window, but does not replenish the recovery budget. */
    public void resumed(long now) { paused = false; progressAt = now; }

    public void retryLater(long now) { retryAt = now + 1000L; }

    /** An unverified BACK must not produce an endless sequence of navigation actions. */
    public void openingFailed() { blocked = true; }

    public void completed(long now, long unpausedPairDurationMs) {
        if (unpausedPairDurationMs > 0) {
            long sample = Math.min(1000L, unpausedPairDurationMs);
            averagePairMs = averagePairMs == 0 ? sample : (averagePairMs * 3L + sample) / 4L;
        }
        progressAt = now;
        retryAt = 0;
        pausesWithoutProgress = 0;
        paused = blocked = false;
    }

    public void reset() {
        progressAt = -1;
        retryAt = averagePairMs = 0;
        pausesWithoutProgress = 0;
        paused = blocked = false;
    }
}
