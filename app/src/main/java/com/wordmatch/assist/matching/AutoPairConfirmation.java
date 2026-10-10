package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;

import java.util.List;

/** Confirms stable disappearance or an increase in the lesson's successful-match counter. */
public final class AutoPairConfirmation {
    private static final long REQUIRED_STABLE_MS = 80L;
    private static final long COUNTER_STABLE_MS = 16L;
    private static final long MAX_OBSERVATION_GAP_MS = 500L;

    private WordBox firstTarget;
    private WordBox secondTarget;
    private int baselineWordCount;
    private int baselineStreak;
    private MatchProgressReader.Reading baselineProgress;
    private int screenWidth;
    private int screenHeight;
    private boolean disappearanceObserved;
    private boolean counterObserved;
    private long disappearanceSinceMs;
    private long lastObservedMs;

    public void start(
            WordBox first,
            WordBox second,
            int baselineWordCount,
            int screenWidth,
            int screenHeight
    ) {
        start(first, second, baselineWordCount, screenWidth, screenHeight, -1);
    }

    public void start(WordBox first, WordBox second, int baselineWordCount,
                      int screenWidth, int screenHeight, int baselineStreak) {
        start(first, second, baselineWordCount, screenWidth, screenHeight, baselineStreak, null);
    }

    public void start(WordBox first, WordBox second, int baselineWordCount,
                      int screenWidth, int screenHeight, int baselineStreak,
                      MatchProgressReader.Reading baselineProgress) {
        reset();
        if (first == null || second == null || baselineWordCount < 2
                || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }
        firstTarget = first;
        secondTarget = second;
        this.baselineWordCount = baselineWordCount;
        this.baselineStreak = baselineStreak;
        this.baselineProgress = baselineProgress;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public boolean observe(List<WordBox> words, boolean completeMatchingBoard, long nowMs) {
        return observe(words, completeMatchingBoard, -1, nowMs);
    }

    public boolean hasSuccessEvidence(List<WordBox> words, int streak) {
        return hasSuccessEvidence(words, streak, null);
    }

    public boolean hasSuccessEvidence(List<WordBox> words, int streak, MatchProgressReader.Reading progress) {
        return firstTarget != null && secondTarget != null && words != null
                && (counterAdvanced(streak, progress)
                || AutoPairCompletionDetector.areBothTargetsGone(
                words, firstTarget, secondTarget, baselineWordCount, screenWidth, screenHeight));
    }

    public boolean observe(List<WordBox> words, boolean completeMatchingBoard, int streak, long nowMs) {
        return observe(words, completeMatchingBoard, streak, null, nowMs);
    }

    public boolean observe(List<WordBox> words, boolean completeMatchingBoard, int streak,
                           MatchProgressReader.Reading progress, long nowMs) {
        if (firstTarget == null || secondTarget == null) {
            return false;
        }
        if (!completeMatchingBoard || !hasSuccessEvidence(words, streak, progress)) {
            clearDisappearance();
            return false;
        }
        // Confirm the counter on the next frame, without waiting for the cards'
        // exit animation. Disappearance still needs the longer empty-frame guard.
        boolean counterAdvanced = counterAdvanced(streak, progress);
        if (!disappearanceObserved
                || counterObserved != counterAdvanced
                || nowMs < lastObservedMs
                || nowMs - lastObservedMs > MAX_OBSERVATION_GAP_MS) {
            disappearanceObserved = true;
            counterObserved = counterAdvanced;
            disappearanceSinceMs = nowMs;
            lastObservedMs = nowMs;
            return false;
        }
        lastObservedMs = nowMs;
        return nowMs - disappearanceSinceMs >= (counterAdvanced ? COUNTER_STABLE_MS : REQUIRED_STABLE_MS);
    }

    public void reset() {
        firstTarget = null;
        secondTarget = null;
        baselineWordCount = 0;
        baselineStreak = -1;
        baselineProgress = null;
        screenWidth = 0;
        screenHeight = 0;
        clearDisappearance();
    }

    private void clearDisappearance() {
        disappearanceObserved = false;
        counterObserved = false;
        disappearanceSinceMs = 0L;
        lastObservedMs = 0L;
    }

    private boolean counterAdvanced(int streak, MatchProgressReader.Reading progress) {
        return (baselineStreak >= 0 && streak > baselineStreak)
                || (progress != null && progress.advancedFrom(baselineProgress));
    }
}
