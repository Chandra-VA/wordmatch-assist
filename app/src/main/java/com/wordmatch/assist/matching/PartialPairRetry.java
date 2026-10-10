package com.wordmatch.assist.matching;

import static com.wordmatch.assist.matching.CardVisualState.State.*;
import com.wordmatch.assist.matching.CardVisualState.State;

/** Retry only the missing half of an active pair, after stable positive feedback. */
public final class PartialPairRetry {
    public enum Action { NONE, FIRST, SECOND, RECHECK }
    private Action candidate = Action.NONE;
    private long since = -1, last = -1, lastRetry = -1;
    private int attempts;

    public Action observe(State first, State second, long clickedAt, long now) {
        Action next = first == SELECTED && second == READY ? Action.SECOND
                : first == READY && second == SELECTED ? Action.FIRST
                : first == READY && second == READY ? Action.RECHECK : Action.NONE;
        long minimumAge = next == Action.RECHECK ? 450L : 160L;
        if (next == Action.NONE || attempts >= 2 || now < clickedAt
                || now - clickedAt < minimumAge || (lastRetry >= 0 && now - lastRetry < 160L)) {
            clearObservation();
            return Action.NONE;
        }
        if (candidate != next || last < 0 || now < last || now - last > 600L) {
            candidate = next;
            since = now;
        }
        last = now;
        return now - since >= 48L ? next : Action.NONE;
    }

    public void recordRetry(long now) {
        attempts++;
        lastRetry = now;
        clearObservation();
    }

    public void clearObservation() { candidate = Action.NONE; since = -1; last = -1; }
    public void reset() { clearObservation(); attempts = 0; lastRetry = -1; }
}
