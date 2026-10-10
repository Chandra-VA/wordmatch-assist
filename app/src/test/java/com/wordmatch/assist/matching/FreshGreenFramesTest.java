package com.wordmatch.assist.matching;
import org.junit.Test;
import static org.junit.Assert.*;
public final class FreshGreenFramesTest {
    @Test public void staleFirstFrameAndSingleGreenFrameNeverConfirm() {
        FreshGreenFrames frames = new FreshGreenFrames();
        assertFalse(frames.observe(1, true, 100));
        assertFalse(frames.observe(1, true, 116));
        assertFalse(frames.observe(1, true, 131));
        assertTrue(frames.observe(1, true, 132));
        assertFalse(frames.observe(2, true, 148));
    }
    @Test public void whiteFrameGapOrCancellationRestartsConfirmation() {
        FreshGreenFrames frames = new FreshGreenFrames();
        frames.observe(1, false, 0);
        frames.observe(1, true, 16);
        assertFalse(frames.observe(1, false, 32));
        assertFalse(frames.observe(1, true, 48));
        assertFalse(frames.observe(1, true, 400));
        assertFalse(frames.observe(1, true, 416));
        assertTrue(frames.observe(1, true, 432));
        frames.reset();
        assertFalse(frames.observe(1, true, 448));
    }
}
