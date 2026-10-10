package com.wordmatch.assist.matching;

/** Pure decision rule for releasing clicks after Duolingo's pause dialog closes. */
public final class PauseDismissalPolicy {
    private PauseDismissalPolicy() {
    }

    public static boolean isReady(
            boolean confirmationDialogVisible,
            int visibleWordCount,
            boolean matchingPromptVisible,
            long dialogAbsentForMs,
            long requiredBoardStableMs,
            long requiredEmptyStableMs
    ) {
        return !confirmationDialogVisible
                && (matchingPromptVisible || visibleWordCount == 0)
                && ((visibleWordCount >= 2
                && dialogAbsentForMs >= requiredBoardStableMs)
                || dialogAbsentForMs >= requiredEmptyStableMs);
    }
}
