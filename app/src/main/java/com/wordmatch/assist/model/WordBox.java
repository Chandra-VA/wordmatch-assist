package com.wordmatch.assist.model;

import com.wordmatch.assist.matching.WordNormalizer;

import java.util.Objects;

/** A piece of recognized text together with its screen-space bounding box. */
public final class WordBox {
    private final String text;
    private final String normalized;
    private final int left;
    private final int top;
    private final int right;
    private final int bottom;

    public WordBox(String text, int left, int top, int right, int bottom) {
        this.text = text == null ? "" : text.trim();
        this.normalized = WordNormalizer.normalize(this.text);
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public String getText() {
        return text;
    }

    public String getNormalized() {
        return normalized;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getRight() {
        return right;
    }

    public int getBottom() {
        return bottom;
    }

    public int getCenterX() {
        return left + (right - left) / 2;
    }

    public int getCenterY() {
        return top + (bottom - top) / 2;
    }

    public int getWidth() {
        return Math.max(0, right - left);
    }

    public int getHeight() {
        return Math.max(0, bottom - top);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WordBox)) {
            return false;
        }
        WordBox box = (WordBox) other;
        return left == box.left
                && top == box.top
                && right == box.right
                && bottom == box.bottom
                && normalized.equals(box.normalized);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalized, left, top, right, bottom);
    }
}
