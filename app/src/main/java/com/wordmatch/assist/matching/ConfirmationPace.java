package com.wordmatch.assist.matching;

import java.util.ArrayDeque;

/** Wall-clock confirmed-pair throughput; pause time remains in the denominator. */
public final class ConfirmationPace {
    private final ArrayDeque<Long> completions = new ArrayDeque<>();
    private long startedAt = -1L;
    public void start(long now) { if (startedAt < 0L) startedAt = now; }
    public void confirmed(long now) { start(now); trim(now); completions.addLast(now); }
    public double perSecond(long now) {
        if (startedAt < 0L || now - startedAt < 1000L) return Double.NaN;
        trim(now);
        return completions.size() * 1000.0 / Math.min(3000L, now - startedAt);
    }
    private void trim(long now) {
        while (!completions.isEmpty() && completions.peekFirst() <= now - 3000L) completions.removeFirst();
    }
    public void reset() { startedAt = -1L; completions.clear(); }
}
