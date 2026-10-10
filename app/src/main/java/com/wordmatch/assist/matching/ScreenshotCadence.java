package com.wordmatch.assist.matching;

/** Respect device-specific screenshot throttling without repeatedly hammering a rejected interval. */
public final class ScreenshotCadence {
    private long interval = 350L, lastRequest = -1;
    public boolean canRequest(long now) { return lastRequest < 0 || now - lastRequest >= interval; }
    public void requested(long now) { lastRequest = now; }
    public void throttled(long now) {
        interval = Math.min(1100L, interval + 250L);
        lastRequest = now;
    }
}
