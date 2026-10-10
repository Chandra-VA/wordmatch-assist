package com.wordmatch.assist.ai;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;
import java.util.ArrayList;
import java.util.List;

/** Unknown cards only. Preserve duplicate occurrences for the response validator. */
public final class AiBoardRequest {
    public final List<String> left = new ArrayList<>();
    public final List<String> right = new ArrayList<>();

    public static AiBoardRequest from(List<WordBox> available, List<MatchPair> known, int width) {
        AiBoardRequest request = new AiBoardRequest();
        for (WordBox word : available) {
            boolean matched = false;
            for (MatchPair pair : known) {
                if (word.equals(pair.getFirst()) || word.equals(pair.getSecond())) { matched = true; break; }
            }
            if (!matched) (word.getCenterX() < width / 2 ? request.left : request.right).add(word.getText());
        }
        return request;
    }

    public boolean needsModel() {
        // A single remaining pair can be resolved locally once known cards finish.
        return !left.isEmpty() && !right.isEmpty() && (left.size() > 1 || right.size() > 1);
    }
}
