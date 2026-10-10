package com.wordmatch.assist.matching;

import java.util.Set;

/** Removes only contradictory learned pairs whose two tiles were on the same observed board. */
public final class VisiblePairConflictPolicy {
    private VisiblePairConflictPolicy() {
    }

    public static boolean shouldRemove(
            String existingFirst,
            String existingSecond,
            String confirmedFirst,
            String confirmedSecond,
            Set<String> visibleNormalizedWords
    ) {
        String oldFirst = WordNormalizer.normalize(existingFirst);
        String oldSecond = WordNormalizer.normalize(existingSecond);
        String newFirst = WordNormalizer.normalize(confirmedFirst);
        String newSecond = WordNormalizer.normalize(confirmedSecond);
        if (oldFirst.isEmpty()
                || oldSecond.isEmpty()
                || newFirst.isEmpty()
                || newSecond.isEmpty()
                || visibleNormalizedWords == null) {
            return false;
        }
        boolean samePair = (oldFirst.equals(newFirst) && oldSecond.equals(newSecond))
                || (oldFirst.equals(newSecond) && oldSecond.equals(newFirst));
        if (samePair) {
            return false;
        }
        boolean sharesConfirmedWord = oldFirst.equals(newFirst)
                || oldFirst.equals(newSecond)
                || oldSecond.equals(newFirst)
                || oldSecond.equals(newSecond);
        return sharesConfirmedWord
                && visibleNormalizedWords.contains(oldFirst)
                && visibleNormalizedWords.contains(oldSecond);
    }
}
