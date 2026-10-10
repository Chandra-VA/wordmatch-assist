package com.wordmatch.assist.accessibility;

import android.graphics.Bitmap;
import com.wordmatch.assist.matching.CardVisualState;
import com.wordmatch.assist.model.WordBox;

/** Copies only one card region to CPU memory. No OCR, files, or network. */
final class GreenCardScreenshot {
    private GreenCardScreenshot() {}

    static CardVisualState.State readState(Bitmap screenshot, WordBox screenCard, WordBox screenText,
                                          int originX, int originY) {
        int left = screenCard.getLeft() - originX;
        int top = screenCard.getTop() - originY;
        int width = screenCard.getWidth();
        int height = screenCard.getHeight();
        if (left < 0 || top < 0 || width <= 0 || height <= 0
                || left + width > screenshot.getWidth() || top + height > screenshot.getHeight()) {
            return CardVisualState.State.UNKNOWN;
        }
        Bitmap crop = null;
        Bitmap pixels = null;
        try {
            crop = Bitmap.createBitmap(screenshot, left, top, width, height);
            pixels = crop.copy(Bitmap.Config.ARGB_8888, false);
            if (pixels == null) return CardVisualState.State.UNKNOWN;
            WordBox text = new WordBox(screenText.getText(),
                    screenText.getLeft() - screenCard.getLeft(), screenText.getTop() - screenCard.getTop(),
                    screenText.getRight() - screenCard.getLeft(), screenText.getBottom() - screenCard.getTop());
            return CardVisualState.read(pixels::getPixel, width, height,
                    new WordBox("", 0, 0, width, height), text);
        } finally {
            if (pixels != null) pixels.recycle();
            if (crop != null && crop != screenshot) crop.recycle();
        }
    }
}
