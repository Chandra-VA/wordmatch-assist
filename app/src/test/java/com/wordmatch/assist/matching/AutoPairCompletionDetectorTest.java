package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AutoPairCompletionDetectorTest {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1600;
    private static final WordBox LEFT = box("酸的", 100, 300);
    private static final WordBox RIGHT = box("sour", 700, 300);

    @Test
    public void acceptsOnlyAfterBothClickedTargetsDisappear() {
        List<WordBox> replacementBoard = Arrays.asList(
                box("方式", 100, 400),
                box("way", 700, 400),
                box("讲座", 100, 300),
                box("lecture", 700, 300)
        );

        assertTrue(AutoPairCompletionDetector.areBothTargetsGone(
                replacementBoard,
                LEFT,
                RIGHT,
                4,
                WIDTH,
                HEIGHT
        ));
    }

    @Test
    public void rejectsWhenOnlyOneClickedTargetDisappeared() {
        List<WordBox> partialTransition = Arrays.asList(
                RIGHT,
                box("方式", 100, 400),
                box("way", 700, 400)
        );

        assertFalse(AutoPairCompletionDetector.areBothTargetsGone(
                partialTransition,
                LEFT,
                RIGHT,
                4,
                WIDTH,
                HEIGHT
        ));
    }

    @Test
    public void rejectsTemporaryWholeBoardDisappearance() {
        assertFalse(AutoPairCompletionDetector.areBothTargetsGone(
                Collections.emptyList(),
                LEFT,
                RIGHT,
                4,
                WIDTH,
                HEIGHT
        ));
    }

    @Test
    public void acceptsEmptyBoardAfterTheFinalPairDisappears() {
        assertTrue(AutoPairCompletionDetector.areBothTargetsGone(
                Collections.emptyList(), LEFT, RIGHT, 2, WIDTH, HEIGHT
        ));
    }

    @Test
    public void waitsUntilBothFinalTargetsDisappear() {
        assertFalse(AutoPairCompletionDetector.areBothTargetsGone(
                Arrays.asList(LEFT, RIGHT), LEFT, RIGHT, 2, WIDTH, HEIGHT
        ));
        assertFalse(AutoPairCompletionDetector.areBothTargetsGone(
                Collections.singletonList(RIGHT), LEFT, RIGHT, 2, WIDTH, HEIGHT
        ));
    }

    private static WordBox box(String text, int x, int y) {
        return new WordBox(text, x, y, x + 120, y + 50);
    }
}
