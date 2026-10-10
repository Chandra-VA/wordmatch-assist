package com.wordmatch.assist.learning;

import com.wordmatch.assist.model.WordBox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Confirms that the exact left and right tiles clicked by the user were accepted. */
public final class ManualPairValidator {
    private ManualPairValidator() {
    }

    public static List<WordBox> findCompletedPair(
            WordBox leftTarget,
            WordBox rightTarget,
            List<WordBox> baseline,
            List<WordBox> current,
            int screenWidth,
            int screenHeight
    ) {
        if (leftTarget == null
                || rightTarget == null
                || baseline == null
                || current == null
                || baseline.size() < 2
                || leftTarget.getCenterX() >= screenWidth / 2
                || rightTarget.getCenterX() < screenWidth / 2) {
            return Collections.emptyList();
        }
        boolean finalPairCompleted = baseline.size() == 2 && current.isEmpty();
        if (!finalPairCompleted
                && current.size() < Math.max(2, baseline.size() - 2)) {
            return Collections.emptyList();
        }
        if (containsTarget(current, leftTarget, screenWidth, screenHeight)
                || containsTarget(current, rightTarget, screenWidth, screenHeight)) {
            return Collections.emptyList();
        }
        return Arrays.asList(leftTarget, rightTarget);
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
