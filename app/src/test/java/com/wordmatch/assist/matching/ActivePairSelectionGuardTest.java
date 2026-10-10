package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ActivePairSelectionGuardTest {
    private final WordBox first = new WordBox("靴子",100,300,150,330);
    private final WordBox second = new WordBox("boot",700,300,750,330);
    private final WordBox other = new WordBox("团",100,500,150,530);
    private final List<WordBox> selected = Collections.singletonList(other);

    @Test public void normalFirstOrSecondSelectionNeverRedirectsTheActivePair() {
        assertFalse(ActivePairSelectionGuard.isUnexpected(Collections.singletonList(first),first,second));
        assertFalse(ActivePairSelectionGuard.isUnexpected(Collections.singletonList(second),first,second));
        assertFalse(ActivePairSelectionGuard.isUnexpected(Collections.emptyList(),first,second));
        assertFalse(ActivePairSelectionGuard.isUnexpected(Arrays.asList(first,other),first,second));
    }

    @Test public void redirectRequiresStableSelectionAfterClickSettlement() {
        ActivePairSelectionGuard guard = new ActivePairSelectionGuard();
        assertFalse(guard.observe(selected,first,second,1000,1159));
        assertFalse(guard.observe(selected,first,second,1000,1160));
        assertFalse(guard.observe(selected,first,second,1000,1207));
        assertTrue(guard.observe(selected,first,second,1000,1208));
    }

    @Test public void transientSelectionAndLongGapsCannotRedirect() {
        ActivePairSelectionGuard guard = new ActivePairSelectionGuard();
        assertFalse(guard.observe(selected,first,second,1000,1160));
        assertFalse(guard.observe(Collections.emptyList(),first,second,1000,1180));
        assertFalse(guard.observe(selected,first,second,1000,1208));
        assertFalse(guard.observe(selected,first,second,1000,1800));
        assertTrue(guard.observe(selected,first,second,1000,1848));
        guard.reset();
        assertFalse(guard.observe(selected,first,second,1000,1900));
    }

    @Test public void identicalTextOnAnotherCardIsStillAnUnexpectedSelection() {
        WordBox duplicate = new WordBox("靴子",100,500,150,530);
        assertTrue(ActivePairSelectionGuard.isUnexpected(Collections.singletonList(duplicate),first,second));
    }
}
