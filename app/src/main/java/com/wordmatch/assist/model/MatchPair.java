package com.wordmatch.assist.model;

/** One high-confidence, one-to-one match displayed with a shared label. */
public final class MatchPair {
    private final WordBox first;
    private final WordBox second;
    private final double score;
    private final int label;

    public MatchPair(WordBox first, WordBox second, double score, int label) {
        this.first = first;
        this.second = second;
        this.score = score;
        this.label = label;
    }

    public WordBox getFirst() {
        return first;
    }

    public WordBox getSecond() {
        return second;
    }

    public double getScore() {
        return score;
    }

    public int getLabel() {
        return label;
    }
}
