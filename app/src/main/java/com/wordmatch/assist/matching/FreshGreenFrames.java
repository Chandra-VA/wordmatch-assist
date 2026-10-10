package com.wordmatch.assist.matching;

/** Discard the first frame of a new pair, then require consecutive green frames. */
public final class FreshGreenFrames {
    private int generation = -1;
    private long since = -1, last = -1;
    public boolean observe(int currentGeneration, boolean green, long now) {
        if (generation != currentGeneration) {
            generation = currentGeneration; since = -1; last = now; return false;
        }
        if (!green || now < last || now - last > 250L) { since = -1; last = now; return false; }
        last = now;
        if (since < 0) { since = now; return false; }
        return now - since >= 16L;
    }
    public void reset() { generation = -1; since = -1; last = -1; }
}
