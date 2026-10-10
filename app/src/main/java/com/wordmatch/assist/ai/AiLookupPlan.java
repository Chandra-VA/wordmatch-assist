package com.wordmatch.assist.ai;

import com.wordmatch.assist.matching.ExactPairSelector;
import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;

import java.util.List;

/** Keep translation knowledge separate from temporary clickability and completion state. */
public final class AiLookupPlan {
    public final AiBoardRequest request;
    public final int localPairCount;

    private AiLookupPlan(AiBoardRequest request, int localPairCount) {
        this.request = request;
        this.localPairCount = localPairCount;
    }

    public static AiLookupPlan from(List<MatchPair> visibleMatches,
                                    List<WordBox> remaining, List<WordBox> active, int width) {
        List<MatchPair> known = ExactPairSelector.allExact(visibleMatches);
        int pending = 0;
        for (MatchPair pair : known) {
            if (remaining.contains(pair.getFirst()) && remaining.contains(pair.getSecond())) pending++;
        }
        // Removing a disabled/completed partner must not turn its known translation into
        // a new AI question. Only truly unmatched active cards belong in the request.
        return new AiLookupPlan(AiBoardRequest.from(active, known, width), pending);
    }

    public boolean hasLocalPairs() { return localPairCount > 0; }
}
