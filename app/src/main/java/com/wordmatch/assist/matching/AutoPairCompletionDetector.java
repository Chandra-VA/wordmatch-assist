package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;

import java.util.List;

/** Prevents a new automatic pair from starting before Duolingo accepts the previous pair. */
public final class AutoPairCompletionDetector {
    private AutoPairCompletionDetector() {
    }

    public static boolean areBothTargetsGone(
            List<WordBox> currentWords,
            WordBox firstTarget,
            WordBox secondTarget,
            int baselineWordCount,
            int screenWidth,
            int screenHeight
    ) {
        if (currentWords == null
                || firstTarget == null
                || secondTarget == null
                || baselineWordCount < 2
                || currentWords.size() < (baselineWordCount == 2 ? 0 : Math.max(2, baselineWordCount - 2))) {
            return false;
        }
        return !containsTarget(currentWords, firstTarget, screenWidth, screenHeight)
                && !containsTarget(currentWords, secondTarget, screenWidth, screenHeight);
    }

    private static boolean containsTarget(
            List<WordBox> words,
            WordBox target,
            int screenWidth,
            int screenHeight
    ) {
        int maximumX = Math.max(32, Math.round(screenWidth * 0.04f));
        int maximumY = Math.max(32, Math.round(screenHeight * 0.04f));
        for (WordBox word : words) {
            if (target.getNormalized().equals(word.getNormalized())
                    && Math.abs(target.getCenterX() - word.getCenterX()) <= maximumX
                    && Math.abs(target.getCenterY() - word.getCenterY()) <= maximumY) {
                return true;
            }
        }
        return false;
    }
}
