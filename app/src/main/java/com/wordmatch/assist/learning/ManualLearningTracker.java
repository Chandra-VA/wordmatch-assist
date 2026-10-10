package com.wordmatch.assist.learning;

import com.wordmatch.assist.model.WordBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Groups rapid user clicks into immutable two-tile attempts.
 *
 * <p>Once one left and one right tile have been observed, that pair is moved to
 * the pending queue. A click for the next pair can therefore never overwrite
 * only one half of the previous pair while its disappearance animation runs.</p>
 */
public final class ManualLearningTracker {
    private static final int MAX_PENDING_ATTEMPTS = 8;
    private static final long MAX_ONE_SIDED_DISAPPEARANCE_MS = 180L;

    private final List<Attempt> pendingAttempts = new ArrayList<>();
    private final List<WordBox> activeBaseline = new ArrayList<>();
    private WordBox activeLeft;
    private WordBox activeRight;
    private long activeLastClickAt;

    public void recordClick(
            WordBox clicked,
            List<WordBox> currentBoard,
            int screenWidth,
            long now
    ) {
        if (clicked == null) {
            return;
        }
        if (activeBaseline.isEmpty()) {
            if (currentBoard != null) {
                activeBaseline.addAll(currentBoard);
            }
        }
        if (clicked.getCenterX() < screenWidth / 2) {
            activeLeft = clicked;
        } else {
            activeRight = clicked;
        }
        activeLastClickAt = now;
        if (activeLeft == null || activeRight == null) {
            return;
        }
        pendingAttempts.add(new Attempt(
                activeLeft,
                activeRight,
                new ArrayList<>(activeBaseline),
                now
        ));
        while (pendingAttempts.size() > MAX_PENDING_ATTEMPTS) {
            pendingAttempts.remove(0);
        }
        clearActive();
    }

    public void pruneExpired(long now, long maximumAgeMs) {
        for (int index = pendingAttempts.size() - 1; index >= 0; index--) {
            if (now - pendingAttempts.get(index).createdAt > maximumAgeMs) {
                pendingAttempts.remove(index);
            }
        }
        if (activeLastClickAt > 0L && now - activeLastClickAt > maximumAgeMs) {
            clearActive();
        }
    }

    public boolean hasWork(long now, long maximumAgeMs) {
        pruneExpired(now, maximumAgeMs);
        return activeLeft != null || activeRight != null || !pendingAttempts.isEmpty();
    }

    public List<Attempt> getPendingAttempts() {
        return new ArrayList<>(pendingAttempts);
    }

    public Attempt getNewestPendingAttempt() {
        return pendingAttempts.isEmpty()
                ? null
                : pendingAttempts.get(pendingAttempts.size() - 1);
    }

    public void remove(Attempt attempt) {
        pendingAttempts.remove(attempt);
    }

    public WordBox getActiveLeft() {
        return activeLeft;
    }

    public WordBox getActiveRight() {
        return activeRight;
    }

    public boolean hasPendingAttempts() {
        return !pendingAttempts.isEmpty();
    }

    public void clear() {
        pendingAttempts.clear();
        clearActive();
    }

    private void clearActive() {
        activeBaseline.clear();
        activeLeft = null;
        activeRight = null;
        activeLastClickAt = 0L;
    }

    public enum Observation {
        WAITING,
        COMPLETED,
        REJECTED
    }

    public static final class Attempt {
        private final WordBox left;
        private final WordBox right;
        private final List<WordBox> baseline;
        private final long createdAt;
        private long oneSideMissingSince;

        private Attempt(
                WordBox left,
                WordBox right,
                List<WordBox> baseline,
                long createdAt
        ) {
            this.left = left;
            this.right = right;
            this.baseline = Collections.unmodifiableList(baseline);
            this.createdAt = createdAt;
        }

        public WordBox getLeft() {
            return left;
        }

        public WordBox getRight() {
            return right;
        }

        public List<WordBox> getBaseline() {
            return baseline;
        }

        public Observation observe(
                List<WordBox> current,
                int screenWidth,
                int screenHeight,
                long now
        ) {
            boolean leftVisible = containsTarget(current, left, screenWidth, screenHeight);
            boolean rightVisible = containsTarget(current, right, screenWidth, screenHeight);
            if (leftVisible && rightVisible) {
                oneSideMissingSince = 0L;
                return Observation.WAITING;
            }
            if (leftVisible != rightVisible) {
                if (oneSideMissingSince == 0L) {
                    oneSideMissingSince = now;
                }
                return now - oneSideMissingSince > MAX_ONE_SIDED_DISAPPEARANCE_MS
                        ? Observation.REJECTED
                        : Observation.WAITING;
            }
            if (oneSideMissingSince > 0L
                    && now - oneSideMissingSince > MAX_ONE_SIDED_DISAPPEARANCE_MS) {
                return Observation.REJECTED;
            }
            return ManualPairValidator.findCompletedPair(
                    left,
                    right,
                    baseline,
                    current,
                    screenWidth,
                    screenHeight
            ).size() == 2 ? Observation.COMPLETED : Observation.WAITING;
        }

        private static boolean containsTarget(
                List<WordBox> words,
                WordBox target,
                int screenWidth,
                int screenHeight
        ) {
            if (words == null || target == null) {
                return false;
            }
            int maximumX = Math.max(32, Math.round(screenWidth * 0.04f));
            int maximumY = Math.max(32, Math.round(screenHeight * 0.04f));
            for (WordBox word : words) {
                if (target.getNormalized().equals(word.getNormalized())
                        && Math.abs(target.getCenterX() - word.getCenterX()) <= maximumX
                        && Math.abs(target.getCenterY() - word.getCenterY()) <= maximumY) {
                    return true;
                }
            }
            return false;
        }
    }
}
