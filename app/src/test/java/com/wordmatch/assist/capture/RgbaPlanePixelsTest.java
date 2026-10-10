package com.wordmatch.assist.capture;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RgbaPlanePixelsTest {
    @Test public void readsRgbaWithRowPaddingAndBufferOffset() {
        ByteBuffer buffer = ByteBuffer.allocate(64);
        buffer.position(4);
        buffer.put(4 + 16 + 4, (byte) 0xD7);
        buffer.put(4 + 16 + 5, (byte) 0xFF);
        buffer.put(4 + 16 + 6, (byte) 0xB8);
        buffer.put(4 + 16 + 7, (byte) 0xFF);
        RgbaPlanePixels pixels = new RgbaPlanePixels(buffer, 3, 2, 16, 4);
        assertEquals(0xFFD7FFB8, pixels.colorAt(1, 1));
        assertEquals(4, buffer.position());
        assertEquals(0, pixels.colorAt(3, 0));
        assertEquals(0, pixels.colorAt(-1, 0));
    }
    @Test public void truncatedPlaneDoesNotReadPastBuffer() {
        assertEquals(0, new RgbaPlanePixels(ByteBuffer.allocate(2), 3, 3, 12, 4).colorAt(1, 1));
    }
}
