package com.wordmatch.assist.matching;

/** Called after the original dispatch queue drains; grey/unknown cards need not belong to the batch. */
public final class BurstPausePolicy {
    private BurstPausePolicy() {
    }

    public static boolean shouldPauseForReplacementWords(
            int visibleBoardWordCount,
            int issuedPairCount
    ) {
        return visibleBoardWordCount > 2
                && issuedPairCount > 0;
    }
}
