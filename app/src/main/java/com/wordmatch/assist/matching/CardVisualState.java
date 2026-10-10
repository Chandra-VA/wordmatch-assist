package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;

/** Positive pixel evidence; faint labels require separate node/transition corroboration. */
public final class CardVisualState {
    public enum State { UNKNOWN, READY, SELECTED, GREEN, FADED }

    private CardVisualState() {}

    public static State read(GreenCardDetector.Pixels pixels, int width, int height,
                             WordBox card, WordBox text) {
        if (pixels == null || card == null
                || card.getWidth() < 20 || card.getHeight() < 12
                || card.getLeft() < 0 || card.getTop() < 0
                || card.getRight() > width || card.getBottom() > height) return State.UNKNOWN;
        if (GreenCardDetector.isGreenCard(pixels, width, height, card)) return State.GREEN;
        if (text == null || text.getLeft() < card.getLeft() || text.getRight() > card.getRight()
                || text.getTop() < card.getTop() || text.getBottom() > card.getBottom()
                || text.getWidth() < 2 || text.getHeight() < 2) return State.UNKNOWN;
        int white = 0, blue = 0;
        for (int[] point : GreenCardDetector.samplePoints(card)) {
            int color = pixels.colorAt(point[0], point[1]);
            int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
            if ((color >>> 24) >= 220 && Math.min(r, Math.min(g, b)) >= 235
                    && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 15) white++;
            if ((color >>> 24) >= 220 && r >= 160 && g >= 210 && b >= 235
                    && g - r >= 12 && b - r >= 25 && b - g >= 5) blue++;
        }
        if (blue >= 11) return State.SELECTED;
        if (white < 11) return State.UNKNOWN;
        // Ignore the badge and border outside the text box. Grey completed labels
        // cannot qualify; hue-tinted dark glyphs under our highlight still can.
        int dark = 0, faded = 0, total = 0;
        int insetX = Math.max(1, text.getWidth() / 10);
        int insetY = Math.max(1, text.getHeight() / 10);
        int step = Math.max(1, text.getHeight() / 24);
        for (int y = text.getTop() + insetY; y < text.getBottom() - insetY; y += step) {
            for (int x = text.getLeft() + insetX; x < text.getRight() - insetX; x += step) {
                int color = pixels.colorAt(x, y);
                int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
                total++;
                if ((color >>> 24) >= 220 && Math.max(r, Math.max(g, b)) <= 165) dark++;
                if ((color >>> 24) >= 220 && Math.min(r, Math.min(g, b)) >= 175
                        && Math.max(r, Math.max(g, b)) <= 240) faded++;
            }
        }
        if (dark >= Math.max(5, total / 50)) return State.READY;
        // Faint glyphs are evidence for a separate disabled-card transition check,
        // never permission to click and never success on their own.
        return faded >= Math.max(8, total / 30) ? State.FADED : State.UNKNOWN;
    }
}
