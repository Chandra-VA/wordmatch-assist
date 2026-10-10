package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;
import java.util.List;

/** Stop waiting on the old pair when a different selected card is consistently observed. */
public final class ActivePairSelectionGuard {
    private WordBox candidate;
    private long since, last;

    public static boolean isUnexpected(List<WordBox> selected, WordBox first, WordBox second) {
        return selected.size() == 1 && !same(selected.get(0), first) && !same(selected.get(0), second);
    }

    public boolean observe(List<WordBox> selected, WordBox first, WordBox second, long clickedAt, long now) {
        if (!isUnexpected(selected, first, second) || now < clickedAt || now - clickedAt < 160L) {
            reset();
            return false;
        }
        WordBox current = selected.get(0);
        if (!same(current, candidate) || now < last || now - last > 500L) {
            candidate = current;
            since = now;
        }
        last = now;
        return now - since >= 48L;
    }

    public void reset() { candidate = null; since = last = 0; }

    private static boolean same(WordBox a, WordBox b) {
        return a != null && b != null && a.getNormalized().equals(b.getNormalized())
                && Math.abs((long) a.getCenterX() - b.getCenterX()) <= 4
                && Math.abs((long) a.getCenterY() - b.getCenterY()) <= 4;
    }
}
