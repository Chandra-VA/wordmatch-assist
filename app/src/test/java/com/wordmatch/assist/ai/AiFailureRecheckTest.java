package com.wordmatch.assist.ai;

import com.wordmatch.assist.model.WordBox;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public final class AiFailureRecheckTest {
    private List<WordBox> board() {
        return Arrays.asList(new WordBox("a",100,300,180,330),new WordBox("b",100,400,180,430),
                new WordBox("c",700,300,780,330),new WordBox("d",700,400,780,430));
    }

    @Test public void failureWaitsForFreshStableObservationsNotRequestDuration() {
        AiFailureRecheck check = new AiFailureRecheck();
        assertFalse(check.isStable(board(),1000,true,10_000));
        assertFalse(check.isStable(board(),1000,true,10_159));
        assertTrue(check.isStable(board(),1000,true,10_160));
    }

    @Test public void modalAndIncompleteScanCannotJustifyTakeover() {
        AiFailureRecheck check = new AiFailureRecheck();
        assertFalse(check.isStable(board(),1000,true,1000));
        assertFalse(check.isStable(board(),1000,false,1160));
        assertFalse(check.isStable(board(),1000,true,1200));
        assertTrue(check.isStable(board(),1000,true,1360));
    }

    @Test public void replacementOrMovingCardStartsANewObservationWindow() {
        AiFailureRecheck check = new AiFailureRecheck();
        assertFalse(check.isStable(board(),1000,true,1000));
        List<WordBox> changed = new ArrayList<>(board());
        changed.set(0,new WordBox("new",100,300,180,330));
        assertFalse(check.isStable(changed,1000,true,1160));
        changed.set(0,new WordBox("new",100,310,180,340));
        assertFalse(check.isStable(changed,1000,true,1320));
        assertTrue(check.isStable(changed,1000,true,1480));
    }

    @Test public void enumerationOrderAloneDoesNotDelayRecovery() {
        AiFailureRecheck check = new AiFailureRecheck();
        assertFalse(check.isStable(board(),1000,true,1000));
        List<WordBox> reordered = new ArrayList<>(board());
        Collections.reverse(reordered);
        assertTrue(check.isStable(reordered,1000,true,1160));
    }

    @Test public void rotationObservationGapAndClockRollbackRequireFreshEvidence() {
        AiFailureRecheck check = new AiFailureRecheck();
        assertFalse(check.isStable(board(),1000,true,1000));
        assertFalse(check.isStable(board(),1200,true,1160));
        assertFalse(check.isStable(board(),1200,true,2000));
        assertFalse(check.isStable(board(),1200,true,1900));
        assertTrue(check.isStable(board(),1200,true,2060));
    }

    @Test public void emptyOrFinalPairCannotTriggerAiTakeover() {
        AiFailureRecheck check = new AiFailureRecheck();
        assertFalse(check.isStable(board(),1000,true,1000));
        assertFalse(check.isStable(Collections.emptyList(),1000,true,1160));
        assertFalse(check.isStable(Arrays.asList(board().get(0),board().get(2)),1000,true,1320));
    }

    @Test public void findingLocalPairsOrCancellingMustDiscardEarlierFailureEvidence() {
        AiFailureRecheck check = new AiFailureRecheck();
        assertFalse(check.isStable(board(),1000,true,1000));
        assertTrue(check.isStable(board(),1000,true,1160));
        check.reset();
        assertFalse(check.isStable(board(),1000,true,1200));
        assertTrue(check.isStable(board(),1000,true,1360));
    }
}
