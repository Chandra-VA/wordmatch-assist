package com.wordmatch.assist.accessibility;

import com.wordmatch.assist.matching.MatchingExerciseDetector;
import java.text.Normalizer;

/** Screen-coordinate filtering shared with the extraction regression tests. */
final class TileTextPolicy {
    private TileTextPolicy() {}

    static boolean accepts(String text, int left, int top, int right, int bottom,
                           int width, int height) {
        if (text == null || text.isEmpty() || text.length() > 50
                || width <= 0 || height <= 0
                || MatchingExerciseDetector.isMatchingPromptText(text)) {
            return false;
        }
        String compact = Normalizer.normalize(text, Normalizer.Form.NFKC).replaceAll("\\s+", "");
        if (compact.matches("(?:连击次数|連擊次數):?[0-9]*")) {
            return false;
        }
        if (top < height * 0.12f || bottom > height * 0.96f
                || right <= left || bottom <= top
                || right - left > width * 0.48f || bottom - top > height * 0.12f
                || Math.abs(((long) left + right) / 2.0 - width / 2.0) < width * 0.035f) {
            return false;
        }
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (Character.isLetter(codePoint)) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }
}
