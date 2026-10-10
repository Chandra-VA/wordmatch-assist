package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;

import java.util.List;
import java.util.ArrayList;

/** Allows automatic actions only for explicit, exact vocabulary entries. */
public final class ExactPairSelector {
    private static final double EXACT_SCORE = 0.999999;

    private ExactPairSelector() {
    }

    public static MatchPair firstExact(List<MatchPair> matches) {
        if (matches == null) {
            return null;
        }
        for (MatchPair match : matches) {
            if (match != null && match.getScore() >= EXACT_SCORE) {
                return match;
            }
        }
        return null;
    }

    public static List<MatchPair> allExact(List<MatchPair> matches) {
        List<MatchPair> exact = new ArrayList<>();
        if (matches == null) {
            return exact;
        }
        for (MatchPair match : matches) {
            if (match != null && match.getScore() >= EXACT_SCORE) {
                exact.add(match);
            }
        }
        return exact;
    }
}
