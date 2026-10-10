package com.wordmatch.assist.matching;

import java.text.Normalizer;
import java.util.Locale;

/** Collects evidence for the lower-screen leave-confirmation sheet in one node scan. */
public final class PauseConfirmationDetector {
    private boolean hasWarning;
    private double highestResumeCenter = Double.POSITIVE_INFINITY;
    private double lowestExitCenter = Double.NEGATIVE_INFINITY;

    public void observe(
            String text,
            int left,
            int top,
            int right,
            int bottom,
            int screenWidth,
            int screenHeight
    ) {
        if (!isLowerCenteredNode(left, top, right, bottom, screenWidth, screenHeight)) {
            return;
        }
        String normalized = compact(text);
        if (normalized.contains("要是现在离开")
                || normalized.contains("进度就白跑")
                || (normalized.contains("现在离开")
                && normalized.contains("经验进度就没了"))) {
            hasWarning = true;
        }
        double centerY = ((double) top + bottom) / (2.0 * screenHeight);
        if (isResumeLabel(normalized)) {
            highestResumeCenter = Math.min(highestResumeCenter, centerY);
        }
        if ("退出".equals(normalized)
                || "exit".equals(normalized)
                || "quit".equals(normalized)) {
            lowestExitCenter = Math.max(lowestExitCenter, centerY);
        }
    }

    public boolean isVisible() {
        boolean hasResume = highestResumeCenter != Double.POSITIVE_INFINITY;
        // The sheet's exit action is below its resume action, unlike same-row word cards.
        return hasResume && (hasWarning || lowestExitCenter > highestResumeCenter + 0.015);
    }

    /** Only identifies a candidate; callers must also confirm isVisible() before clicking. */
    public static boolean isResumeControl(
            String text,
            int left,
            int top,
            int right,
            int bottom,
            int screenWidth,
            int screenHeight
    ) {
        return isLowerCenteredNode(left, top, right, bottom, screenWidth, screenHeight)
                && isResumeLabel(compact(text));
    }

    private static boolean isResumeLabel(String normalized) {
        return "返回".equals(normalized)
                || "继续".equals(normalized)
                || "continue".equals(normalized)
                || "resume".equals(normalized);
    }

    private static boolean isLowerCenteredNode(
            int left,
            int top,
            int right,
            int bottom,
            int screenWidth,
            int screenHeight
    ) {
        if (screenWidth <= 0 || screenHeight <= 0 || right <= left || bottom <= top
                || right <= 0 || bottom <= 0 || left >= screenWidth || top >= screenHeight) {
            return false;
        }
        double centerX = ((double) left + right) / 2.0;
        double centerY = ((double) top + bottom) / 2.0;
        // No minimum width: accessibility often exposes only the narrow text leaf.
        // Stay inside the central 40% so labels in the two word-card columns are excluded.
        return Math.abs(centerX - screenWidth / 2.0) <= screenWidth * 0.20
                && centerY >= screenHeight * 0.62
                && centerY < screenHeight
                && (double) bottom - top <= screenHeight * 0.25;
    }

    private static String compact(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(normalized.length());
        for (int offset = 0; offset < normalized.length(); ) {
            int codePoint = normalized.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) {
                result.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }
}
