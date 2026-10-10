package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;
import java.util.List;

/** The all-grey tail can be checked only when every visible tile was actually submitted. */
public final class SubmittedBoardEvidence {
    private SubmittedBoardEvidence() {}
    public static boolean covers(List<WordBox> words, List<MatchPair> submitted) {
        if (words == null || words.size() < 2 || submitted == null) return false;
        for (WordBox word : words) {
            boolean found = false;
            for (MatchPair pair : submitted) {
                if (same(word, pair.getFirst()) || same(word, pair.getSecond())) { found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }
    private static boolean same(WordBox a, WordBox b) {
        return a != null && b != null && a.getNormalized().equals(b.getNormalized())
                && Math.abs((long)a.getCenterX() - b.getCenterX()) <= 4L
                && Math.abs((long)a.getCenterY() - b.getCenterY()) <= 4L;
    }
}
