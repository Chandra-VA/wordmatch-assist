package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;
import java.util.ArrayList;
import java.util.List;

/** A short, bounded exception for the exact cards just verified ready in a screenshot. */
public final class VisualPairRetry {
    private final List<MatchPair> attempts = new ArrayList<>();
    private MatchPair active;
    private long until;

    public boolean canRetry(MatchPair pair) {
        int count = 0;
        for (MatchPair previous : attempts) if (samePair(previous, pair)) count++;
        return count < 2;
    }

    public boolean authorize(MatchPair pair, long now) {
        if (!canRetry(pair)) return false;
        attempts.add(pair);
        active = pair;
        until = now + 1500L;
        return true;
    }

    public boolean permits(MatchPair pair, long now) {
        return active != null && now < until && samePair(active, pair);
    }

    public boolean permits(WordBox word, long now) {
        return active != null && now < until
                && (same(active.getFirst(), word) || same(active.getSecond(), word));
    }

    public void clearActive() { active = null; }

    public void pruneMissing(List<WordBox> words) {
        if (words == null || words.size() < 2) return;
        for (int i = attempts.size() - 1; i >= 0; i--) {
            MatchPair pair = attempts.get(i);
            if (!contains(words, pair.getFirst()) && !contains(words, pair.getSecond())) attempts.remove(i);
        }
    }

    public void reset() { clearActive(); attempts.clear(); }

    private static boolean contains(List<WordBox> words, WordBox target) {
        for (WordBox word : words) if (same(word, target)) return true;
        return false;
    }

    private static boolean samePair(MatchPair a, MatchPair b) {
        return (same(a.getFirst(), b.getFirst()) && same(a.getSecond(), b.getSecond()))
                || (same(a.getFirst(), b.getSecond()) && same(a.getSecond(), b.getFirst()));
    }

    private static boolean same(WordBox a, WordBox b) {
        return a.getNormalized().equals(b.getNormalized())
                && Math.abs(a.getLeft() - b.getLeft()) <= 4 && Math.abs(a.getRight() - b.getRight()) <= 4
                && Math.abs(a.getTop() - b.getTop()) <= 4 && Math.abs(a.getBottom() - b.getBottom()) <= 4;
    }
}
