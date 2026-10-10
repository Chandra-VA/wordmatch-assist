package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;

import java.util.List;

/** Finishes a selected tile's exact pair before allowing another pair to start. */
public final class SelectedPairPlanner {
    private SelectedPairPlanner() {
    }

    public static Plan choose(
            List<MatchPair> exactPairs,
            List<WordBox> selectedWords,
            int screenWidth,
            int screenHeight
    ) {
        int selectedCount = selectedWords == null ? 0 : selectedWords.size();
        if (selectedCount > 1) {
            return null;
        }
        WordBox selected = selectedCount == 1 ? selectedWords.get(0) : null;
        if (selectedCount == 1 && !hasText(selected)) {
            return null;
        }

        for (MatchPair pair : ExactPairSelector.allExact(exactPairs)) {
            if (!hasText(pair.getFirst()) || !hasText(pair.getSecond())) {
                continue;
            }
            if (selectedCount == 0) {
                return new Plan(pair, false);
            }
            if (matches(selected, pair.getFirst(), screenWidth, screenHeight)) {
                return new Plan(pair, true);
            }
            if (matches(selected, pair.getSecond(), screenWidth, screenHeight)) {
                MatchPair reordered = new MatchPair(
                        pair.getSecond(), pair.getFirst(), pair.getScore(), pair.getLabel());
                return new Plan(reordered, true);
            }
        }
        // A selection without a known partner must not trigger clicks on an unrelated pair.
        return null;
    }

    private static boolean hasText(WordBox word) {
        return word != null && !word.getNormalized().isEmpty();
    }

    private static boolean matches(
            WordBox selected,
            WordBox target,
            int screenWidth,
            int screenHeight
    ) {
        int maximumX = Math.max(32, Math.round(screenWidth * 0.04f));
        int maximumY = Math.max(32, Math.round(screenHeight * 0.04f));
        return selected.getNormalized().equals(target.getNormalized())
                && Math.abs((long) selected.getCenterX() - target.getCenterX()) <= maximumX
                && Math.abs((long) selected.getCenterY() - target.getCenterY()) <= maximumY;
    }

    public static final class Plan {
        private final MatchPair pair;
        private final boolean firstAlreadySelected;

        private Plan(MatchPair pair, boolean firstAlreadySelected) {
            this.pair = pair;
            this.firstAlreadySelected = firstAlreadySelected;
        }

        public MatchPair getPair() {
            return pair;
        }

        public boolean isFirstAlreadySelected() {
            return firstAlreadySelected;
        }
    }
}
