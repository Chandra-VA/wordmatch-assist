package com.wordmatch.assist.matching;

import java.util.Locale;

/** Strict allowlist for the Duolingo matching-exercise page title. */
public final class MatchingExerciseDetector {
    private static final String[] COMPACT_MARKERS = {
            "选择配对",
            "選擇配對",
            "选择匹配",
            "選擇匹配",
            "matchthepairs",
            "matchpairs",
            "selectthematchingpairs"
    };

    private MatchingExerciseDetector() {
    }

    /** Uses the same title text and location rules for extraction and click guards. */
    public static boolean isMatchingPromptNode(
            String text,
            String description,
            int left,
            int top,
            int right,
            int bottom,
            int width,
            int height
    ) {
        return width > 0 && height > 0
                && right > left && bottom > top
                && ((long) left + right) / 2.0f < width * 0.75f
                && ((long) top + bottom) / 2.0f < height * 0.30f
                && (isMatchingPromptText(text) || isMatchingPromptText(description));
    }

    public static boolean isMatchingPromptText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String compact = compact(value);
        for (String marker : COMPACT_MARKERS) {
            if (compact.equals(marker)) {
                return true;
            }
        }
        return false;
    }

    private static String compact(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder output = new StringBuilder(lower.length());
        for (int offset = 0; offset < lower.length(); ) {
            int codePoint = lower.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) {
                output.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return output.toString();
    }
}
