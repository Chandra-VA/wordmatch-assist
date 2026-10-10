package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the matching lesson's feedback counter, including split accessibility nodes. */
public final class MatchStreakReader {
    private static final Pattern LABEL = Pattern.compile("^(?:连击次数|連擊次數|combo|streak):?([0-9]{1,6})?$");
    private final List<WordBox> labels = new ArrayList<>();
    private final List<WordBox> numbers = new ArrayList<>();
    private int count = -1;
    private boolean conflicting;

    public void observe(String text, int left, int top, int right, int bottom, int width, int height) {
        if (text == null || width <= 0 || height <= 0 || right <= left || bottom <= top
                || left < 0 || right > width * 0.65f || top < height * 0.04f
                || bottom > height * 0.30f || bottom - top > height * 0.08f) {
            return;
        }
        String compact = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        Matcher label = LABEL.matcher(compact);
        if (label.matches()) {
            if (label.group(1) != null) {
                addCount(Integer.parseInt(label.group(1)));
            } else {
                labels.add(new WordBox(compact, left, top, right, bottom));
            }
        } else if (compact.matches("[0-9]{1,6}") && right - left <= width * 0.15f) {
            numbers.add(new WordBox(compact, left, top, right, bottom));
        }
    }

    public int getCount(int width) {
        for (WordBox label : labels) {
            for (WordBox number : numbers) {
                if (number.getLeft() >= label.getRight() - 4
                        && number.getLeft() - label.getRight() <= width * 0.12f
                        && Math.abs(number.getCenterY() - label.getCenterY())
                        <= Math.max(number.getHeight(), label.getHeight()) * 0.6f) {
                    addCount(Integer.parseInt(number.getText()));
                }
            }
        }
        return conflicting ? -1 : count;
    }

    private void addCount(int value) {
        if (count >= 0 && count != value) {
            conflicting = true;
        }
        count = value;
    }
}
