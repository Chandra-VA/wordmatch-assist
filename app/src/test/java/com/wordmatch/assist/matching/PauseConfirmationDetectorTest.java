package com.wordmatch.assist.matching;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PauseConfirmationDetectorTest {
    @Test
    public void detectsScreenshotWarningAndNarrowResumeTextLeaf() {
        PauseConfirmationDetector detector = new PauseConfirmationDetector();
        detector.observe("现在离开的话，你的经验进度就没了！",
                306, 1648, 896, 1680, 1200, 1920);
        assertFalse(detector.isVisible());
        detector.observe("返回", 576, 1730, 625, 1757, 1200, 1920);
        assertTrue(detector.isVisible());
        assertTrue(PauseConfirmationDetector.isResumeControl(
                "返回", 576, 1730, 625, 1757, 1200, 1920));
    }

    @Test
    public void detectsNarrowResumeAndExitWithoutWarningInEitherScanOrder() {
        PauseConfirmationDetector detector = new PauseConfirmationDetector();
        detector.observe("退出", 576, 1828, 625, 1855, 1200, 1920);
        assertFalse(detector.isVisible());
        detector.observe("返回", 576, 1730, 625, 1757, 1200, 1920);
        assertTrue(detector.isVisible());

        detector = new PauseConfirmationDetector();
        detector.observe("返回", 576, 1730, 625, 1757, 1200, 1920);
        assertFalse(detector.isVisible());
        detector.observe("退出", 576, 1828, 625, 1855, 1200, 1920);
        assertTrue(detector.isVisible());
    }

    @Test
    public void detectsBothPreviouslySupportedWarnings() {
        for (String warning : new String[]{"要是现在离开，你就会失去进度", "进度就白跑了！"}) {
            PauseConfirmationDetector detector = new PauseConfirmationDetector();
            detector.observe(warning, 200, 1500, 1000, 1580, 1200, 1920);
            detector.observe("继续", 22, 1710, 1178, 1782, 1200, 1920);
            assertTrue(warning, detector.isVisible());
        }
    }

    @Test
    public void normalizesCaseWhitespaceAndPunctuationForExactControlLabels() {
        for (String resume : new String[]{" 返回 ", "继续！", " CONTINUE\n", "ＲＥＳＵＭＥ"}) {
            PauseConfirmationDetector detector = new PauseConfirmationDetector();
            detector.observe(resume, 450, 1700, 750, 1780, 1200, 1920);
            detector.observe("Quit", 550, 1830, 650, 1870, 1200, 1920);
            assertTrue(resume, detector.isVisible());
            assertTrue(PauseConfirmationDetector.isResumeControl(
                    resume, 450, 1700, 750, 1780, 1200, 1920));
        }
    }

    @Test
    public void rejectsIndividualLabelsAndWarningAlone() {
        for (String text : new String[]{"返回", "退出", "again", "现在离开的话，你的经验进度就没了！"}) {
            PauseConfirmationDetector detector = new PauseConfirmationDetector();
            detector.observe(text, 200, 1500, 1000, 1580, 1200, 1920);
            assertFalse(text, detector.isVisible());
        }
    }

    @Test
    public void rejectsWarningWithOnlyExit() {
        PauseConfirmationDetector detector = new PauseConfirmationDetector();
        detector.observe("现在离开的话，你的经验进度就没了！",
                306, 1648, 896, 1680, 1200, 1920);
        detector.observe("退出", 576, 1828, 625, 1855, 1200, 1920);
        assertFalse(detector.isVisible());
    }

    @Test
    public void rejectsTopNavigationBackEvenWithExitBelow() {
        PauseConfirmationDetector detector = new PauseConfirmationDetector();
        detector.observe("返回", 0, 20, 160, 100, 1200, 1920);
        detector.observe("退出", 576, 1828, 625, 1855, 1200, 1920);
        assertFalse(detector.isVisible());
        assertFalse(PauseConfirmationDetector.isResumeControl(
                "返回", 0, 20, 160, 100, 1200, 1920));
        assertFalse(PauseConfirmationDetector.isResumeControl(
                "继续", 540, 100, 660, 160, 1200, 1920));
    }

    @Test
    public void rejectsNonExactResumeText() {
        for (String text : new String[]{null, "", "返回首页", "继续学习", "resume lesson", "discontinue"}) {
            PauseConfirmationDetector detector = new PauseConfirmationDetector();
            detector.observe(text, 400, 1700, 800, 1780, 1200, 1920);
            detector.observe("退出", 576, 1828, 625, 1855, 1200, 1920);
            assertFalse(detector.isVisible());
            assertFalse(PauseConfirmationDetector.isResumeControl(
                    text, 400, 1700, 800, 1780, 1200, 1920));
        }
    }

    @Test
    public void rejectsWordColumnLabelsAndSameRowActionWords() {
        PauseConfirmationDetector detector = new PauseConfirmationDetector();
        detector.observe("返回", 275, 1500, 325, 1540, 1200, 1920);
        detector.observe("退出", 875, 1700, 925, 1740, 1200, 1920);
        assertFalse(detector.isVisible());

        detector = new PauseConfirmationDetector();
        detector.observe("返回", 450, 1700, 500, 1740, 1200, 1920);
        detector.observe("退出", 700, 1700, 750, 1740, 1200, 1920);
        assertFalse(detector.isVisible());
    }

    @Test
    public void rejectsExitAboveResume() {
        PauseConfirmationDetector detector = new PauseConfirmationDetector();
        detector.observe("返回", 576, 1828, 625, 1855, 1200, 1920);
        detector.observe("退出", 576, 1730, 625, 1757, 1200, 1920);
        assertFalse(detector.isVisible());
    }

    @Test
    public void usesScreenProportionsAcrossPhoneAndTabletSizes() {
        for (int[] size : new int[][]{{360, 640}, {1200, 1920}, {2560, 1600}}) {
            int width = size[0];
            int height = size[1];
            PauseConfirmationDetector detector = new PauseConfirmationDetector();
            detector.observe("continue", width * 45 / 100, height * 88 / 100,
                    width * 55 / 100, height * 91 / 100, width, height);
            detector.observe("exit", width * 48 / 100, height * 95 / 100,
                    width * 52 / 100, height * 97 / 100, width, height);
            assertTrue(width + "x" + height, detector.isVisible());
        }
    }

    @Test
    public void rejectsInvalidOrOffscreenBounds() {
        int[][] bounds = {
                {600, 1700, 600, 1750}, {550, 1750, 650, 1700},
                {550, 1930, 650, 1970}, {1300, 1700, 1400, 1750},
                {0, 0, 1200, 1920}
        };
        for (int[] rect : bounds) {
            assertFalse(PauseConfirmationDetector.isResumeControl(
                    "返回", rect[0], rect[1], rect[2], rect[3], 1200, 1920));
        }
        assertFalse(PauseConfirmationDetector.isResumeControl(
                "返回", 550, 1700, 650, 1750, 0, 1920));
        assertFalse(PauseConfirmationDetector.isResumeControl(
                "返回", 550, 1700, 650, 1750, 1200, 0));
    }
}
