package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Supplements missing accessibility selection flags with fresh, stable card colors. */
public final class VisualBoardSelection {
    private List<WordBox> words = Collections.emptyList(), cards = Collections.emptyList();
    private List<WordBox> selected = Collections.emptyList();
    private int width, height;
    private long preparedAt, observedAt = -1, since;

    public void prepare(List<WordBox> words, List<WordBox> cards, int width, int height, long now) {
        if (!this.words.equals(words) || !this.cards.equals(cards) || this.width != width || this.height != height) {
            clearObservation();
            this.words = new ArrayList<>(words);
            this.cards = new ArrayList<>(cards);
            this.width = width;
            this.height = height;
        }
        preparedAt = now;
    }

    public void observe(GreenCardDetector.Pixels pixels, int width, int height, long now) {
        if (pixels == null || width != this.width || height != this.height || now < preparedAt
                || now - preparedAt > 250L || words.isEmpty() || words.size() > 24
                || cards.size() != words.size() || cards.contains(null)) {
            clearObservation();
            return;
        }
        List<WordBox> current = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            if (CardVisualState.read(pixels, width, height, cards.get(i), words.get(i))
                    == CardVisualState.State.SELECTED) current.add(words.get(i));
        }
        record(current, now);
    }

    /** A screenshot must still describe the exact board and rectangles in the latest node scan. */
    public void observeSelected(List<WordBox> capturedWords, List<WordBox> capturedCards,
                                int width, int height, List<WordBox> selected, long now) {
        if (!words.equals(capturedWords) || !cards.equals(capturedCards) || this.width != width
                || this.height != height || cards.contains(null) || cards.size() != words.size()
                || now < preparedAt || now - preparedAt > 250L || !words.containsAll(selected)) return;
        record(new ArrayList<>(selected), now);
    }

    private void record(List<WordBox> current, long now) {
        if (!selected.equals(current) || observedAt < 0 || now < observedAt || now - observedAt > 600L) {
            selected = current;
            since = now;
        }
        observedAt = now;
    }

    public List<WordBox> selectedWords(long now) {
        return observedAt >= 0 && now >= observedAt && now - observedAt <= 120L && now - since >= 48L
                ? new ArrayList<>(selected) : Collections.emptyList();
    }

    public void reset() {
        words = cards = Collections.emptyList();
        width = height = 0;
        clearObservation();
    }

    private void clearObservation() {
        selected = Collections.emptyList();
        observedAt = -1;
        since = 0;
    }
}
