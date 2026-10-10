package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.WordBox;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.wordmatch.assist.matching.CardVisualState.State.*;

public final class CardVisualStateTest {
    private static final WordBox CARD = new WordBox("", 0, 0, 400, 70);
    private static final WordBox TEXT = new WordBox("word", 180, 24, 224, 48);

    @Test public void recordedAcceptPairTurnsGreenThenFadedAndNeverBecomesClickableAgain() throws Exception {
        byte[] rgb;
        try (InputStream input = new GZIPInputStream(getClass().getResourceAsStream("/accept-faded-sequence.rgb.gz"))) {
            rgb = input.readAllBytes();
        }
        int frameSize = 800 * 64 * 3;
        assertEquals(10 * frameSize, rgb.length);
        FadedPairConfirmation confirmation = new FadedPairConfirmation();
        for (int frame = 0; frame < 10; frame++) {
            final int start = frame * frameSize;
            GreenCardDetector.Pixels pixels = (x, y) -> {
                int offset = start + (y * 800 + x) * 3;
                return 0xFF000000 | ((rgb[offset] & 255) << 16) | ((rgb[offset + 1] & 255) << 8) | (rgb[offset + 2] & 255);
            };
            CardVisualState.State left = CardVisualState.read(pixels, 800, 64,
                    new WordBox("", 16, 0, 389, 64), new WordBox("接受", 183, 21, 221, 44));
            CardVisualState.State right = CardVisualState.read(pixels, 800, 64,
                    new WordBox("", 411, 0, 785, 64), new WordBox("accept", 571, 21, 627, 44));
            assertEquals("left frame " + frame, frame < 3 ? GREEN : FADED, left);
            assertEquals("right frame " + frame, frame < 3 ? GREEN : FADED, right);
            // Node flags are explicit simulated corroboration; they are not available in the recording.
            boolean complete = confirmation.observe(left, right, true, false, false, true, true, 1000, 1160 + frame * 250L);
            assertEquals("confirmation frame " + frame, frame >= 4, complete);
        }
    }

    @Test public void recordingOfLectureStallSelectsOnlyItsUntouchedPartner() throws Exception {
        byte[] rgb = new byte[800 * 396 * 3];
        try (InputStream input = new GZIPInputStream(getClass().getResourceAsStream("/partial-lecture-board.rgb.gz"))) {
            int offset = 0, count;
            while (offset < rgb.length && (count = input.read(rgb, offset, rgb.length - offset)) != -1) offset += count;
            assertEquals(rgb.length, offset);
        }
        GreenCardDetector.Pixels pixels = (x, y) -> {
            int offset = (y * 800 + x) * 3;
            return 0xFF000000 | ((rgb[offset] & 255) << 16) | ((rgb[offset + 1] & 255) << 8) | (rgb[offset + 2] & 255);
        };
        CardVisualState.State first = CardVisualState.read(pixels, 800, 396,
                new WordBox("", 16, 0, 389, 66), new WordBox("讲座", 184, 23, 222, 46));
        CardVisualState.State second = CardVisualState.read(pixels, 800, 396,
                new WordBox("", 411, 82, 785, 147), new WordBox("lecture", 573, 101, 625, 126));
        assertEquals(SELECTED, first);
        assertEquals(READY, second);
        PartialPairRetry retry = new PartialPairRetry();
        assertEquals(PartialPairRetry.Action.NONE, retry.observe(first, second, 1000, 1160));
        assertEquals(PartialPairRetry.Action.SECOND, retry.observe(first, second, 1000, 1208));
    }

    @Test public void aSmallBlueBadgeCannotPretendTheEntireWhiteCardIsSelected() {
        assertEquals(UNKNOWN, read(0xFFFFFFFF, 0xFFDDF4FF));
        assertEquals(UNKNOWN, read(0xFFCCCCCC, 0xFF555555));
        assertEquals(UNKNOWN, CardVisualState.read((x,y) -> 0xFFDDF4FF, 400, 70, CARD, null));
    }

    @Test public void recordingAllowsOnlyMessyPairDespiteAllFiveHighlights() throws Exception {
        byte[] rgb = new byte[800 * 396 * 3];
        try (InputStream input = new GZIPInputStream(getClass().getResourceAsStream("/stalled-highlight-board.rgb.gz"))) {
            int offset = 0, count;
            while (offset < rgb.length && (count = input.read(rgb, offset, rgb.length - offset)) != -1) offset += count;
            assertEquals(rgb.length, offset);
        }
        GreenCardDetector.Pixels image = (x, y) -> {
            int offset = (y * 800 + x) * 3;
            return 0xFF000000 | ((rgb[offset] & 255) << 16) | ((rgb[offset + 1] & 255) << 8) | (rgb[offset + 2] & 255);
        };
        int[] tops = {479, 559, 639, 723, 804};
        int[] textTops = {499, 580, 660, 741, 821};
        int readyCount = 0;
        for (int row = 0; row < 5; row++) {
            WordBox leftCard = new WordBox("", 15, tops[row] - 476, 389, tops[row] + 62 - 476);
            WordBox rightCard = new WordBox("", 411, tops[row] - 476, 785, tops[row] + 62 - 476);
            WordBox left = new WordBox("left", 183, textTops[row] - 476, 221, textTops[row] + 22 - 476);
            WordBox right = new WordBox("right", 577, textTops[row] - 476, 620, textTops[row] + 22 - 476);
            CardVisualState.State l = CardVisualState.read(image, 800, 396, leftCard, left);
            CardVisualState.State r = CardVisualState.read(image, 800, 396, rightCard, right);
            if (row == 1) assertEquals(READY, l);
            else assertTrue("left row " + row, l == UNKNOWN || l == FADED);
            if (row == 2) assertEquals(READY, r);
            else assertTrue("right row " + row, r == UNKNOWN || r == FADED);
            if (l == READY) readyCount++;
            if (r == READY) readyCount++;
        }
        assertEquals(2, readyCount);
    }

    @Test public void greyTextAndBlankCardsCannotAuthorizeClicks() {
        assertEquals(FADED, read(0xFFFFFFFF, 0xFFDADADA));
        assertEquals(UNKNOWN, read(0xFFFFFFFF, 0xFFFFFFFF));
        assertEquals(READY, read(0xFFFFFFFF, 0xFF555555));
    }

    @Test public void selectedWrongAndSuccessfulCardsAreNeverReady() {
        assertEquals(SELECTED, read(0xFFDDF4FF, 0xFF555555));
        assertEquals(UNKNOWN, read(0xFFFFDFE0, 0xFF555555));
        assertEquals(GREEN, read(0xFFD7FFB8, 0xFF555555));
    }

    @Test public void croppedOrMissingTextCannotAuthorizeClicks() {
        assertEquals(UNKNOWN, CardVisualState.read((x,y) -> 0xFFFFFFFF, 399, 70, CARD, TEXT));
        assertEquals(UNKNOWN, CardVisualState.read((x,y) -> 0xFFFFFFFF, 400, 70, CARD, null));
        assertEquals(GREEN, CardVisualState.read((x,y) -> 0xFFD7FFB8, 400, 70, CARD, null));
    }

    private static CardVisualState.State read(int background, int foreground) {
        return CardVisualState.read((x, y) -> x >= 185 && x <= 214 && y >= 28 && y <= 43
                ? foreground : background, 400, 70, CARD, TEXT);
    }
}
