package com.wordmatch.assist.matching;

/** Decides when a previously clicked pair is safe to expose to matching again. */
public final class IssuedPairRetentionPolicy {
    private IssuedPairRetentionPolicy() {
    }

    public static boolean shouldRemove(
            long ageMs,
            long maximumAgeMs,
            int visibleWordCount,
            boolean firstStillVisible,
            boolean secondStillVisible
    ) {
        // Records are created only after confirmed success. A lingering green
        // card must not become clickable again just because its animation is slow.
        if (firstStillVisible || secondStillVisible) {
            return false;
        }
        if (ageMs > maximumAgeMs) {
            return true;
        }
        // A pause/dialog transition temporarily produces zero readable words.
        // That is not evidence that the clicked cards actually disappeared.
        return visibleWordCount >= 2 && !firstStillVisible && !secondStillVisible;
    }
}
