package com.wordmatch.assist.learning;

import com.wordmatch.assist.model.WordBox;

import java.util.ArrayList;
import java.util.List;

/** Detects one completed pair even when replacement tiles already started appearing. */
public final class PairTransitionDetector {
    private PairTransitionDetector() {
    }

    public static boolean didOppositePairDisappear(
            List<WordBox> baseline,
            List<WordBox> current,
            int screenWidth
    ) {
        return findOppositePairDisappear(baseline, current, screenWidth).size() == 2;
    }

    public static List<WordBox> findOppositePairDisappear(
            List<WordBox> baseline,
            List<WordBox> current,
            int screenWidth
    ) {
        if (baseline == null || current == null || baseline.size() < 4) {
            return new ArrayList<>();
        }
        List<String> remaining = new ArrayList<>();
        for (WordBox word : current) {
            remaining.add(word.getNormalized());
        }
        List<WordBox> disappeared = new ArrayList<>();
        for (WordBox oldWord : baseline) {
            int match = remaining.indexOf(oldWord.getNormalized());
            if (match >= 0) {
                remaining.remove(match);
            } else {
                disappeared.add(oldWord);
            }
        }
        if (disappeared.size() != 2) {
            return new ArrayList<>();
        }
        boolean firstIsLeft = disappeared.get(0).getCenterX() < screenWidth / 2;
        boolean secondIsLeft = disappeared.get(1).getCenterX() < screenWidth / 2;
        return firstIsLeft != secondIsLeft ? disappeared : new ArrayList<>();
    }
}
