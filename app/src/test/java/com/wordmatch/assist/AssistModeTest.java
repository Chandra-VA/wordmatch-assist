package com.wordmatch.assist;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AssistModeTest {
    @Test
    public void learningModeNeverClicksOrPauses() {
        assertFalse(AssistMode.LEARNING.isAutoClickEnabled());
        assertFalse(AssistMode.LEARNING.isFreezeEnabled());
    }

    @Test
    public void automaticModeClicksAndUsesFastPause() {
        assertTrue(AssistMode.AUTOMATIC.isAutoClickEnabled());
        assertTrue(AssistMode.AUTOMATIC.isFreezeEnabled());
    }
}
