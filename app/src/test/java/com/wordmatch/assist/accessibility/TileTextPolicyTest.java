package com.wordmatch.assist.accessibility;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TileTextPolicyTest {
    @Test public void portraitScreenshotHasTenCards() {
        String[] left = {"奶奶", "最后", "小孩子", "导航", "保存"};
        String[] right = {"children", "save", "final", "GPS", "granny"};
        for (int i = 0; i < 5; i++) {
            int top = 731 + i * 122;
            assertTrue(TileTextPolicy.accepts(left[i], 23, top, 582, top + 98, 1200, 1920));
            assertTrue(TileTextPolicy.accepts(right[i], 618, top, 1176, top + 98, 1200, 1920));
        }
    }

    @Test public void staleLandscapeDimensionsLoseRightColumnAndBottomCards() {
        assertFalse(TileTextPolicy.accepts("children", 850, 768, 947, 793, 1920, 1200));
        assertFalse(TileTextPolicy.accepts("保存", 276, 1257, 326, 1283, 1920, 1200));
        assertTrue(TileTextPolicy.accepts("children", 850, 768, 947, 793, 1200, 1920));
        assertTrue(TileTextPolicy.accepts("保存", 276, 1257, 326, 1283, 1200, 1920));
    }

    @Test public void excludesHeaderEvenWhenItPassesGeometryFilter() {
        assertFalse(TileTextPolicy.accepts("连击次数：0", 67, 210, 210, 240, 1920, 1200));
        assertFalse(TileTextPolicy.accepts("连击次数：", 67, 210, 180, 240, 1920, 1200));
        assertFalse(TileTextPolicy.accepts("选择配对", 23, 150, 190, 182, 1920, 1200));
        assertTrue(TileTextPolicy.accepts("streak", 618, 731, 1176, 829, 1200, 1920));
    }
}
