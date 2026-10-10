package com.wordmatch.assist.matching;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MatchStreakReaderTest {
    @Test
    public void readsCounterFromRecordedChineseLessonHeader() {
        MatchStreakReader reader = new MatchStreakReader();
        reader.observe("连击次数： 3", 44, 140, 210, 164, 800, 1280);
        assertEquals(3, reader.getCount(800));
    }

    @Test
    public void supportsZeroFullWidthDigitsAndTraditionalChinese() {
        MatchStreakReader reader = new MatchStreakReader();
        reader.observe("連擊次數：０", 44, 140, 210, 164, 800, 1280);
        assertEquals(0, reader.getCount(800));
    }

    @Test
    public void supportsSplitLabelAndNumberRegardlessOfTraversalOrder() {
        MatchStreakReader reader = new MatchStreakReader();
        reader.observe("12", 180, 140, 205, 164, 800, 1280);
        reader.observe("连击次数：", 44, 140, 170, 164, 800, 1280);
        assertEquals(12, reader.getCount(800));
    }

    @Test
    public void identicalTextAndContentDescriptionDoNotConflict() {
        MatchStreakReader reader = new MatchStreakReader();
        reader.observe("连击次数：3", 44, 140, 210, 164, 800, 1280);
        reader.observe("连击次数：3", 44, 140, 210, 164, 800, 1280);
        assertEquals(3, reader.getCount(800));
    }

    @Test
    public void conflictingOldAndNewAnimationNodesReturnUnknown() {
        MatchStreakReader reader = new MatchStreakReader();
        reader.observe("连击次数：2", 44, 140, 210, 164, 800, 1280);
        reader.observe("连击次数：3", 44, 140, 210, 164, 800, 1280);
        assertEquals(-1, reader.getCount(800));
    }

    @Test
    public void rejectsUnrelatedTimerScoreAndCardText() {
        MatchStreakReader reader = new MatchStreakReader();
        reader.observe("1:32", 700, 60, 780, 85, 800, 1280);
        reader.observe("40", 700, 60, 730, 85, 800, 1280);
        reader.observe("经验值：3", 44, 140, 210, 164, 800, 1280);
        reader.observe("连击次数：3", 44, 500, 210, 524, 800, 1280);
        assertEquals(-1, reader.getCount(800));
    }

    @Test
    public void doesNotPairLabelWithANumberOnAnotherRowOrAcrossTheScreen() {
        MatchStreakReader reader = new MatchStreakReader();
        reader.observe("连击次数：", 44, 140, 170, 164, 800, 1280);
        reader.observe("3", 180, 200, 205, 224, 800, 1280);
        reader.observe("4", 480, 140, 505, 164, 800, 1280);
        assertEquals(-1, reader.getCount(800));
    }

    @Test
    public void rejectsNegativeOrMalformedCountsAndOversizedContainers() {
        MatchStreakReader reader = new MatchStreakReader();
        reader.observe("连击次数：-1", 44, 140, 210, 164, 800, 1280);
        reader.observe("连击次数：99999999999", 44, 140, 310, 164, 800, 1280);
        reader.observe("连击次数：3", 0, 0, 800, 1280, 800, 1280);
        assertEquals(-1, reader.getCount(800));
    }
}
