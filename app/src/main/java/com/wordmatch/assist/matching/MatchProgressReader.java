package com.wordmatch.assist.matching;

import java.util.ArrayList;
import java.util.List;

/** Reads the lesson progress bar, never the timer or word-card numbers. */
public final class MatchProgressReader {
    private final List<NumberNode> numbers = new ArrayList<>();

    public void observeText(String text, int left, int top, int right, int bottom,
                            int width, int height) {
        if (!inHeader(left, top, right, bottom, width, height)
                || right - left > width * 0.10f || text == null) {
            return;
        }
        String value = text.trim();
        if (value.matches("[0-9]{1,4}")) {
            numbers.add(new NumberNode(Integer.parseInt(value), left, right, (top + bottom) / 2,
                    bottom - top));
        }
    }

    public Reading getReading(int width) {
        // The recording has a current-count bubble and a separate goal at the
        // right end of the bar. Require both, aligned, and only one current value.
        Reading result = null;
        for (NumberNode goal : numbers) {
            if (goal.left < width * 0.82f || goal.value <= 0) {
                continue;
            }
            for (NumberNode current : numbers) {
                if (current.right > width * 0.80f || current.value > goal.value
                        || Math.abs(current.centerY - goal.centerY)
                        > Math.max(current.height, goal.height) * 0.5f) {
                    continue;
                }
                Reading next = new Reading(current.value, goal.value);
                if (result != null && (result.value != next.value || result.maximum != next.maximum)) {
                    return null;
                }
                result = next;
            }
        }
        return result;
    }

    private static boolean inHeader(int left, int top, int right, int bottom, int width, int height) {
        return width > 0 && height > 0 && right > left && bottom > top
                && left >= width * 0.06f && right <= width * 0.92f
                && top >= height * 0.025f && bottom <= height * 0.10f
                && bottom - top <= height * 0.045f;
    }

    public static final class Reading {
        private final int value;
        private final int maximum;

        private Reading(int value, int maximum) {
            this.value = value;
            this.maximum = maximum;
        }

        public boolean advancedFrom(Reading previous) {
            return previous != null && maximum == previous.maximum
                    && value > previous.value;
        }
    }

    private static final class NumberNode {
        final int value, left, right, centerY, height;
        NumberNode(int value, int left, int right, int centerY, int height) {
            this.value = value;
            this.left = left;
            this.right = right;
            this.centerY = centerY;
            this.height = height;
        }
    }
}
