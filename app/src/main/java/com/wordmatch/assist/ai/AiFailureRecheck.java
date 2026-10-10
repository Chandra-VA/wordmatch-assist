package com.wordmatch.assist.ai;

import com.wordmatch.assist.model.WordBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A failed request can justify takeover only after reobserving a stable, unobscured board. */
public final class AiFailureRecheck {
    private static final long STABLE_MS = 160L;
    private static final long MAX_GAP_MS = 500L;
    private Map<WordBox, Integer> board;
    private int width;
    private long since;
    private long lastObserved;

    public boolean isStable(List<WordBox> words, int screenWidth, boolean complete, long now) {
        if (!complete || words.size() < 3 || screenWidth < 2) {
            reset();
            return false;
        }
        Map<WordBox, Integer> current = new HashMap<>();
        for (WordBox word : words) current.merge(word, 1, Integer::sum);
        if (!current.equals(board) || width != screenWidth || now < lastObserved
                || now - lastObserved > MAX_GAP_MS) {
            board = current;
            width = screenWidth;
            since = now;
        }
        lastObserved = now;
        return now - since >= STABLE_MS;
    }

    public void reset() {
        board = null;
        width = 0;
        since = lastObserved = 0L;
    }
}
