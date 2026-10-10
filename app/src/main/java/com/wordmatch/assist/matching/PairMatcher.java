package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds high-confidence one-to-one matches across the two tile columns. */
public final class PairMatcher {
    private static final double MIN_SCORE = 0.82;
    private static final double EXACT_SCORE = 0.999999;

    private final TranslationLookup lookup;

    public PairMatcher(TranslationLookup lookup) {
        this.lookup = lookup;
    }

    public List<MatchPair> match(List<WordBox> words, int screenWidth) {
        List<Candidate> candidates = new ArrayList<>();
        for (int first = 0; first < words.size(); first++) {
            for (int second = first + 1; second < words.size(); second++) {
                WordBox a = words.get(first);
                WordBox b = words.get(second);
                if (isLeft(a, screenWidth) == isLeft(b, screenWidth)) {
                    continue;
                }
                double score = lookup.score(a.getNormalized(), b.getNormalized());
                if (score >= MIN_SCORE) {
                    candidates.add(new Candidate(first, second, score));
                }
            }
        }

        // Never break an exact-score tie arbitrarily. Old corrupted learning can otherwise
        // make a visible word point at both its correct tile and a wrong tile; the former
        // greedy ordering would auto-click whichever happened to be enumerated first.
        List<Set<String>> exactCounterparts = new ArrayList<>();
        for (int index = 0; index < words.size(); index++) {
            exactCounterparts.add(new HashSet<>());
        }
        for (Candidate candidate : candidates) {
            if (candidate.score >= EXACT_SCORE) {
                exactCounterparts.get(candidate.first).add(
                        words.get(candidate.second).getNormalized()
                );
                exactCounterparts.get(candidate.second).add(
                        words.get(candidate.first).getNormalized()
                );
            }
        }
        List<Candidate> unambiguousCandidates = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate.score >= EXACT_SCORE
                    && (exactCounterparts.get(candidate.first).size() > 1
                    || exactCounterparts.get(candidate.second).size() > 1)) {
                continue;
            }
            unambiguousCandidates.add(candidate);
        }
        candidates = unambiguousCandidates;

        Collections.sort(candidates, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate left, Candidate right) {
                return Double.compare(right.score, left.score);
            }
        });
        Set<Integer> used = new HashSet<>();
        List<Candidate> chosen = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (!used.contains(candidate.first) && !used.contains(candidate.second)) {
                chosen.add(candidate);
                used.add(candidate.first);
                used.add(candidate.second);
            }
        }

        Collections.sort(chosen, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate left, Candidate right) {
                int leftY = Math.min(
                        words.get(left.first).getCenterY(),
                        words.get(left.second).getCenterY()
                );
                int rightY = Math.min(
                        words.get(right.first).getCenterY(),
                        words.get(right.second).getCenterY()
                );
                return Integer.compare(leftY, rightY);
            }
        });

        List<MatchPair> output = new ArrayList<>();
        for (int index = 0; index < chosen.size(); index++) {
            Candidate candidate = chosen.get(index);
            WordBox first = words.get(candidate.first);
            WordBox second = words.get(candidate.second);
            if (!isLeft(first, screenWidth)) {
                WordBox swap = first;
                first = second;
                second = swap;
            }
            output.add(new MatchPair(first, second, candidate.score, index + 1));
        }
        return output;
    }

    private static boolean isLeft(WordBox word, int screenWidth) {
        return word.getCenterX() < screenWidth / 2;
    }

    private static final class Candidate {
        private final int first;
        private final int second;
        private final double score;

        private Candidate(int first, int second, double score) {
            this.first = first;
            this.second = second;
            this.score = score;
        }

    }
}
