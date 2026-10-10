package com.wordmatch.assist.matching;

/** Use observed selection immediately; retain the old delay when no selection is exposed. */
public final class SecondClickTiming {
    public static final long POLL_MS = 8L;
    private static final long FALLBACK_DELAY_MS = 32L;

    private SecondClickTiming() {}

    public static boolean isReady(boolean firstSelected, long elapsedMs) {
        return elapsedMs >= 0 && (firstSelected || elapsedMs >= FALLBACK_DELAY_MS);
    }
}
