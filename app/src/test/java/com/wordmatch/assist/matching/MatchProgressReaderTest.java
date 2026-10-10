package com.wordmatch.assist.matching;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MatchProgressReaderTest {
    @Test public void recordedProgressBubbleAdvancesWithoutAStreakLabel() {
        assertTrue(reading(6, 30).advancedFrom(reading(5, 30)));
        assertFalse(reading(6, 30).advancedFrom(reading(6, 30)));
        assertFalse(reading(5, 30).advancedFrom(reading(6, 30)));
        assertFalse(reading(7, 40).advancedFrom(reading(6, 30)));
        assertFalse(reading(6, 30).advancedFrom(null));
    }

    @Test public void ignoresTimerStatusBarAndWordCards() {
        MatchProgressReader reader = new MatchProgressReader();
        reader.observeText("30", 695, 56, 712, 75, 800, 1280);
        reader.observeText("1:03", 760, 57, 789, 77, 800, 1280);
        reader.observeText("78", 753, 9, 777, 25, 800, 1280);
        reader.observeText("5", 180, 500, 195, 520, 800, 1280);
        reader.observeText("1041", 300, 130, 400, 156, 800, 1280);
        assertNull(reader.getReading(800));
    }

    @Test public void requiresAlignedGoalAndRejectsMultipleCurrentValues() {
        MatchProgressReader reader = new MatchProgressReader();
        reader.observeText("6", 270, 56, 286, 75, 800, 1280);
        assertNull(reader.getReading(800));
        reader.observeText("30", 695, 90, 712, 109, 800, 1280);
        assertNull(reader.getReading(800));
        reader.observeText("30", 695, 56, 712, 75, 800, 1280);
        assertNotNull(reader.getReading(800));
        reader.observeText("5", 260, 56, 276, 75, 800, 1280);
        assertNull(reader.getReading(800));
    }

    @Test public void duplicateTextAndDescriptionAreAllowedButInvalidCountsAreNot() {
        MatchProgressReader reader = new MatchProgressReader();
        reader.observeText("6", 270, 56, 286, 75, 800, 1280);
        reader.observeText("6", 270, 56, 286, 75, 800, 1280);
        reader.observeText("30", 695, 56, 712, 75, 800, 1280);
        assertNotNull(reader.getReading(800));
        assertNull(reading(31, 30));
        assertNull(reading(-1, 30));
        assertNull(reading(0, 0));
    }

    static MatchProgressReader.Reading reading(int current, int goal) {
        MatchProgressReader reader = new MatchProgressReader();
        reader.observeText(Integer.toString(current), 270, 56, 286, 75, 800, 1280);
        reader.observeText(Integer.toString(goal), 695, 56, 712, 75, 800, 1280);
        return reader.getReading(800);
    }
}
