package com.wordmatch.assist.matching;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MatchingExerciseDetectorTest {
    @Test
    public void acceptsKnownMatchingExerciseTitles() {
        assertTrue(MatchingExerciseDetector.isMatchingPromptText("选择配对"));
        assertTrue(MatchingExerciseDetector.isMatchingPromptText("選擇配對"));
        assertTrue(MatchingExerciseDetector.isMatchingPromptText("Match the pairs"));
        assertTrue(MatchingExerciseDetector.isMatchingPromptText("Select the matching pairs:"));
    }

    @Test
    public void rejectsTextsSeenOnProfileCourseAndLeaderboardPages() {
        assertFalse(MatchingExerciseDetector.isMatchingPromptText("钻石等级"));
        assertFalse(MatchingExerciseDetector.isMatchingPromptText(
                "Interview: Talk about job applications"
        ));
        assertFalse(MatchingExerciseDetector.isMatchingPromptText("关注  关注者  课程"));
        assertFalse(MatchingExerciseDetector.isMatchingPromptText("1225 经验"));
        assertFalse(MatchingExerciseDetector.isMatchingPromptText("应用与配对设置"));
    }

    @Test
    public void acceptsTitleDescriptionEvenWithUnrelatedNonemptyText() {
        assertTrue(MatchingExerciseDetector.isMatchingPromptNode(
                "exercise_heading", "选择配对", 22, 151, 200, 188, 1200, 1920
        ));
    }

    @Test
    public void acceptsTitleInEitherNodeTextField() {
        assertTrue(MatchingExerciseDetector.isMatchingPromptNode(
                "选择配对", null, 22, 151, 200, 188, 1200, 1920
        ));
        assertTrue(MatchingExerciseDetector.isMatchingPromptNode(
                null, "Match the pairs", 22, 151, 200, 188, 1200, 1920
        ));
        assertTrue(MatchingExerciseDetector.isMatchingPromptNode(
                " ", "选择配对", 22, 151, 200, 188, 1200, 1920
        ));
    }

    @Test
    public void rejectsTitleTextOutsideHeadingArea() {
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                "选择配对", null, 22, 731, 580, 828, 1200, 1920
        ));
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                null, "选择配对", 950, 151, 1150, 188, 1200, 1920
        ));
    }

    @Test
    public void rejectsEmptyOrUnrelatedNodeText() {
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                null, null, 22, 151, 200, 188, 1200, 1920
        ));
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                " ", "\n", 22, 151, 200, 188, 1200, 1920
        ));
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                "返回", "退出", 22, 151, 200, 188, 1200, 1920
        ));
    }

    @Test
    public void rejectsInvalidNodeAndScreenBounds() {
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                "选择配对", null, 22, 151, 22, 188, 1200, 1920
        ));
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                "选择配对", null, 22, 151, 200, 151, 1200, 1920
        ));
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                "选择配对", null, 22, 151, 200, 188, 0, 1920
        ));
        assertFalse(MatchingExerciseDetector.isMatchingPromptNode(
                "选择配对", null, 22, 151, 200, 188, 1200, 0
        ));
    }
}
