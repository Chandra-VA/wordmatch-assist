package com.wordmatch.assist.matching;

/** Recover a missed green animation using two fresh, corroborated disabled/faded observations. */
public final class FadedPairConfirmation {
    private long since = -1, last = -1;

    public boolean observe(CardVisualState.State first, CardVisualState.State second,
                           boolean startedActive, boolean firstActive, boolean secondActive,
                           boolean otherActiveCard, boolean completeUnselectedBoard,
                           long clickedAt, long now) {
        if (first != CardVisualState.State.FADED || second != CardVisualState.State.FADED
                || !startedActive || firstActive || secondActive || !otherActiveCard
                || !completeUnselectedBoard || clickedAt <= 0 || now < clickedAt || now - clickedAt < 160L) {
            reset();
            return false;
        }
        if (since < 0 || now < last || now - last > 1500L) since = now;
        last = now;
        return now - since >= 96L;
    }

    public void reset() { since = last = -1; }
}
