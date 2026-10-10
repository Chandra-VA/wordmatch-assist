package com.wordmatch.assist.learning;

import com.wordmatch.assist.model.WordBox;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ManualPairValidatorTest {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1600;

    @Test
    public void acceptsOnlyTheExactClickedTargetsWhenTheyDisappear() {
        WordBox clickedLeft = box("essay", 100, 300);
        WordBox clickedRight = box("文章", 700, 300);
        List<WordBox> baseline = Arrays.asList(
                clickedLeft,
                clickedRight,
                box("way", 100, 400),
                box("方式", 700, 400)
        );
        List<WordBox> replacement = Arrays.asList(
                box("way", 100, 400),
                box("方式", 700, 400),
                box("result", 100, 300),
                box("结果", 700, 300)
        );

        List<WordBox> completed = ManualPairValidator.findCompletedPair(
                clickedLeft,
                clickedRight,
                baseline,
                replacement,
                WIDTH,
                HEIGHT
        );

        assertEquals(Arrays.asList(clickedLeft, clickedRight), completed);
    }

    @Test
    public void rejectsWhenDifferentTilesDisappear() {
        WordBox clickedLeft = box("essay", 100, 300);
        WordBox clickedRight = box("方式", 700, 400);
        List<WordBox> current = Arrays.asList(
                clickedLeft,
                clickedRight,
                box("result", 100, 500),
                box("结果", 700, 500)
        );

        assertTrue(ManualPairValidator.findCompletedPair(
                clickedLeft,
                clickedRight,
                current,
                current,
                WIDTH,
                HEIGHT
        ).isEmpty());
    }

    @Test
    public void rejectsWholeBoardDisappearance() {
        WordBox clickedLeft = box("essay", 100, 300);
        WordBox clickedRight = box("文章", 700, 300);
        List<WordBox> baseline = Arrays.asList(
                clickedLeft,
                clickedRight,
                box("way", 100, 400),
                box("方式", 700, 400),
                box("result", 100, 500),
                box("结果", 700, 500)
        );

        assertTrue(ManualPairValidator.findCompletedPair(
                clickedLeft,
                clickedRight,
                baseline,
                Collections.emptyList(),
                WIDTH,
                HEIGHT
        ).isEmpty());
    }

    @Test
    public void acceptsTheLastTwoClickedTilesWhenTheRoundCompletes() {
        WordBox clickedLeft = box("fare", 100, 300);
        WordBox clickedRight = box("车费", 700, 300);

        List<WordBox> completed = ManualPairValidator.findCompletedPair(
                clickedLeft,
                clickedRight,
                Arrays.asList(clickedLeft, clickedRight),
                Collections.emptyList(),
                WIDTH,
                HEIGHT
        );

        assertEquals(Arrays.asList(clickedLeft, clickedRight), completed);
    }

    private static WordBox box(String text, int x, int y) {
        return new WordBox(text, x, y, x + 120, y + 50);
    }
}
