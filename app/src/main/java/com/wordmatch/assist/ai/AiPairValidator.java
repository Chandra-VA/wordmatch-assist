package com.wordmatch.assist.ai;

import com.wordmatch.assist.matching.WordNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps model output constrained to exact, one-to-one words visible on the board. */
public final class AiPairValidator {
    private static final double MIN_CONFIDENCE = 0.92;

    private AiPairValidator() {
    }

    public static List<ValidatedPair> validate(
            List<String> visibleLeft,
            List<String> visibleRight,
            List<CandidatePair> candidates
    ) {
        Map<String, String> leftByNormalized = uniqueWords(visibleLeft);
        Map<String, String> rightByNormalized = uniqueWords(visibleRight);
        Set<String> usedLeft = new HashSet<>();
        Set<String> usedRight = new HashSet<>();
        List<ValidatedPair> result = new ArrayList<>();
        if (candidates == null) {
            return result;
        }
        for (CandidatePair candidate : candidates) {
            if (candidate == null || Double.isNaN(candidate.confidence)
                    || Double.isInfinite(candidate.confidence) || candidate.confidence > 1.0
                    || candidate.confidence < MIN_CONFIDENCE) {
                continue;
            }
            String leftKey = WordNormalizer.normalize(candidate.left);
            String rightKey = WordNormalizer.normalize(candidate.right);
            String exactLeft = leftByNormalized.get(leftKey);
            String exactRight = rightByNormalized.get(rightKey);
            if (exactLeft == null
                    || exactRight == null
                    || usedLeft.contains(leftKey)
                    || usedRight.contains(rightKey)) {
                continue;
            }
            usedLeft.add(leftKey);
            usedRight.add(rightKey);
            result.add(new ValidatedPair(exactLeft, exactRight, candidate.confidence));
        }
        return result;
    }

    private static Map<String, String> uniqueWords(List<String> words) {
        Map<String, String> output = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        if (words == null) {
            return output;
        }
        for (String word : words) {
            String normalized = WordNormalizer.normalize(word);
            if (normalized.isEmpty()) {
                continue;
            }
            if (output.containsKey(normalized)) {
                duplicates.add(normalized);
            } else {
                output.put(normalized, word == null ? "" : word.trim());
            }
        }
        for (String duplicate : duplicates) {
            output.remove(duplicate);
        }
        return output;
    }

    public static final class CandidatePair {
        private final String left;
        private final String right;
        private final double confidence;

        public CandidatePair(String left, String right, double confidence) {
            this.left = left;
            this.right = right;
            this.confidence = confidence;
        }
    }

    public static final class ValidatedPair {
        private final String left;
        private final String right;
        private final double confidence;

        private ValidatedPair(String left, String right, double confidence) {
            this.left = left;
            this.right = right;
            this.confidence = confidence;
        }

        public String getLeft() {
            return left;
        }

        public String getRight() {
            return right;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}
