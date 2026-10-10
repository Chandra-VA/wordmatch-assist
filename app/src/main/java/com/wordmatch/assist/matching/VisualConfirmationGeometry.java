package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;
import java.util.List;

/** Validate a green card even when its completed text has already faded out. */
public final class VisualConfirmationGeometry {
    private VisualConfirmationGeometry() {}

    public static boolean matches(List<WordBox> words, WordBox target, WordBox capturedCard,
                                  WordBox currentCard) {
        if (words == null || target == null || capturedCard == null) return false;
        if (currentCard != null && !sameSlot(capturedCard, currentCard)) return false;
        boolean targetPresent = false;
        for (WordBox word : words) {
            boolean inCard = word.getCenterX() >= capturedCard.getLeft()
                    && word.getCenterX() < capturedCard.getRight()
                    && word.getCenterY() >= capturedCard.getTop()
                    && word.getCenterY() < capturedCard.getBottom();
            if (!inCard) continue;
            // A replacement word or moved label invalidates these captured pixels.
            if (!word.getNormalized().equals(target.getNormalized())
                    || Math.abs(word.getCenterX() - target.getCenterX()) > 4
                    || Math.abs(word.getCenterY() - target.getCenterY()) > 4) return false;
            targetPresent = true;
        }
        // An empty slot is valid only with its unchanged real card control.
        return targetPresent || capturedCard.equals(currentCard);
    }

    private static boolean sameSlot(WordBox a, WordBox b) {
        // Pressed/disabled borders can resize slightly. Background samples sit well
        // inside the card; unchanged target text is still required for this tolerance.
        return Math.abs((long) a.getLeft() - b.getLeft()) <= 4
                && Math.abs((long) a.getRight() - b.getRight()) <= 4
                && Math.abs((long) a.getTop() - b.getTop()) <= 4
                && Math.abs((long) a.getBottom() - b.getBottom()) <= 4;
    }
}
