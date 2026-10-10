package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps one failing pair from stopping other exact pairs on a fresh board. */
public final class AutoPairRecovery {
    public enum Action { WAIT, RESUME, CLEAR_SELECTION, TAKEOVER }
    private static final int MAX_PAIR_FAILURES = 3;
    private static final long SETTLE_MS = 500L;
    private static final long REVIEW_MS = 6000L;
    private final List<Failure> failures = new ArrayList<>();
    private boolean pending;
    private boolean clearAttempted;
    private long settleUntil;
    private long reviewUntil;
    private MatchPair unverifiedSelection;

    public void recordFailure(MatchPair pair, int width, int height, long now) {
        recordFailure(pair, width, height, now, false);
    }

    public void recordFailure(MatchPair pair, int width, int height, long now, boolean partialClick) {
        Failure failure = find(pair, width, height);
        if (failure == null) {
            failure = new Failure(pair);
            failures.add(failure);
        }
        failure.count++;
        unverifiedSelection = partialClick ? pair : null;
        beginReview(now);
    }

    public void beginReview(long now) {
        pending = true;
        clearAttempted = false;
        settleUntil = now + SETTLE_MS;
        reviewUntil = now + REVIEW_MS;
    }

    public boolean isPending() {
        return pending;
    }

    /** The game's paused interval must not spend the fresh-board recovery budget. */
    public void deferForPause(long elapsedMs) {
        if (!pending || elapsedMs <= 0L) return;
        settleUntil += elapsedMs;
        reviewUntil += elapsedMs;
    }

    /** A stable, explicit selection with a known partner already supplies the needed settlement. */
    public void reviewObservedSelectionNow(long now) {
        if (!pending) return;
        unverifiedSelection = null;
        settleUntil = now;
    }

    public boolean isSettled(long now) {
        return pending && now >= settleUntil;
    }

    public boolean inVisualReviewWindow(long now) {
        return isSettled(now) && now < settleUntil + 750L;
    }

    public boolean hasFailed(MatchPair pair, int width, int height) {
        return failureCount(pair, width, height) > 0;
    }

    /** Called only after fresh pixels prove both cards ready and no selection remains. */
    public void resumeVisuallyReadyPair(MatchPair pair, int width, int height) {
        failures.remove(find(pair, width, height));
        unverifiedSelection = null;
        pending = false;
    }

    public List<MatchPair> availablePairs(List<MatchPair> pairs, int width, int height) {
        List<MatchPair> available = new ArrayList<>();
        for (MatchPair pair : ExactPairSelector.allExact(pairs)) {
            if (unverifiedSelection != null && !samePair(pair, unverifiedSelection, width, height)) {
                continue;
            }
            if (failureCount(pair, width, height) < MAX_PAIR_FAILURES) {
                available.add(pair);
            }
        }
        Collections.sort(available, (first, second) -> Integer.compare(
                failureCount(first, width, height), failureCount(second, width, height)));
        return available;
    }

    public Action evaluate(List<MatchPair> executablePairs, List<WordBox> selected,
                           boolean completeBoard, boolean hasKnownPairs,
                           int width, int height, long now) {
        if (!pending) {
            return Action.RESUME;
        }
        // A partial tree or modal must never justify a click, deselection, or takeover.
        if (!completeBoard || now < settleUntil) {
            return Action.WAIT;
        }
        if (selected.size() == 1 || (clearAttempted && selected.isEmpty())) {
            unverifiedSelection = null;
        }
        List<MatchPair> available = availablePairs(executablePairs, width, height);
        if (SelectedPairPlanner.choose(available, selected, width, height) != null) {
            pending = false;
            return Action.RESUME;
        }
        // Clear just the blocking selection once, then require a fresh observation.
        if (selected.size() == 1 && !available.isEmpty() && !clearAttempted) {
            clearAttempted = true;
            settleUntil = now + SETTLE_MS;
            return Action.CLEAR_SELECTION;
        }
        if (!hasKnownPairs && failures.isEmpty()) {
            pending = false;
            return Action.RESUME;
        }
        if (now >= reviewUntil) {
            pending = false;
            return Action.TAKEOVER;
        }
        return Action.WAIT;
    }

    public void recordSuccess(MatchPair pair, int width, int height) {
        Failure failure = find(pair, width, height);
        failures.remove(failure);
        if (unverifiedSelection != null && samePair(pair, unverifiedSelection, width, height)) {
            unverifiedSelection = null;
        }
    }

    /** Call only after a complete matching-page scan. Keep partially disappearing pairs. */
    public void pruneMissing(List<WordBox> words, int width, int height) {
        if (words.size() < 2) {
            return;
        }
        for (int index = failures.size() - 1; index >= 0; index--) {
            Failure failure = failures.get(index);
            if (!contains(words, failure.pair.getFirst(), width, height)
                    && !contains(words, failure.pair.getSecond(), width, height)) {
                failures.remove(index);
            }
        }
        if (unverifiedSelection != null && !contains(words, unverifiedSelection.getFirst(), width, height)
                && !contains(words, unverifiedSelection.getSecond(), width, height)) {
            unverifiedSelection = null;
        }
    }

    public void reset() {
        failures.clear();
        pending = false;
        clearAttempted = false;
        unverifiedSelection = null;
    }

    private int failureCount(MatchPair pair, int width, int height) {
        Failure failure = find(pair, width, height);
        return failure == null ? 0 : failure.count;
    }

    private Failure find(MatchPair pair, int width, int height) {
        for (Failure failure : failures) {
            if (samePair(pair, failure.pair, width, height)) {
                return failure;
            }
        }
        return null;
    }

    private static boolean samePair(MatchPair first, MatchPair second, int width, int height) {
        return (same(first.getFirst(), second.getFirst(), width, height)
                && same(first.getSecond(), second.getSecond(), width, height))
                || (same(first.getFirst(), second.getSecond(), width, height)
                && same(first.getSecond(), second.getFirst(), width, height));
    }

    private static boolean contains(List<WordBox> words, WordBox target, int width, int height) {
        for (WordBox word : words) {
            if (same(word, target, width, height)) {
                return true;
            }
        }
        return false;
    }

    private static boolean same(WordBox first, WordBox second, int width, int height) {
        return first.getNormalized().equals(second.getNormalized())
                && Math.abs((long) first.getCenterX() - second.getCenterX()) <= Math.max(32, width * 0.04f)
                && Math.abs((long) first.getCenterY() - second.getCenterY()) <= Math.max(32, height * 0.04f);
    }

    private static final class Failure {
        final MatchPair pair;
        int count;

        Failure(MatchPair pair) {
            this.pair = pair;
        }
    }
}
