package com.wordmatch.assist.capture;

import java.nio.ByteBuffer;
import com.wordmatch.assist.matching.GreenCardDetector;

/** Read a few pixels directly; never copy or OCR the full display. */
public final class RgbaPlanePixels implements GreenCardDetector.Pixels {
    private final ByteBuffer buffer;
    private final int width, height, rowStride, pixelStride, base;
    public RgbaPlanePixels(ByteBuffer buffer, int width, int height, int rowStride, int pixelStride) {
        this.buffer = buffer;
        this.width = width; this.height = height;
        this.rowStride = rowStride; this.pixelStride = pixelStride;
        base = buffer.position();
    }
    public int colorAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height || pixelStride < 4) return 0;
        long offset = (long) base + (long) y * rowStride + (long) x * pixelStride;
        if (offset < 0 || offset + 3 >= buffer.limit()) return 0;
        int i = (int) offset;
        return ((buffer.get(i + 3) & 255) << 24) | ((buffer.get(i) & 255) << 16)
                | ((buffer.get(i + 1) & 255) << 8) | (buffer.get(i + 2) & 255);
    }
}
