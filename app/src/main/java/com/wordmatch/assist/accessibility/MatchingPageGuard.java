package com.wordmatch.assist.accessibility;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import com.wordmatch.assist.matching.MatchingExerciseDetector;

import java.util.ArrayDeque;

/** Bounded page check used immediately before any injected action. */
final class MatchingPageGuard {
    private static final int MAX_NODES = 2400;

    private MatchingPageGuard() {
    }

    static boolean hasMatchingPrompt(AccessibilityNodeInfo root, int width, int height) {
        if (root == null) {
            return false;
        }
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        try {
            while (!queue.isEmpty() && visited < MAX_NODES) {
                AccessibilityNodeInfo node = queue.removeFirst();
                visited++;
                try {
                    Rect bounds = new Rect();
                    node.getBoundsInScreen(bounds);
                    CharSequence text = node.getText();
                    CharSequence description = node.getContentDescription();
                    if (MatchingExerciseDetector.isMatchingPromptNode(
                            text == null ? null : text.toString(),
                            description == null ? null : description.toString(),
                            bounds.left, bounds.top, bounds.right, bounds.bottom,
                            width, height
                    )) {
                        return true;
                    }
                    for (int index = 0; index < node.getChildCount(); index++) {
                        AccessibilityNodeInfo child = node.getChild(index);
                        if (child != null) {
                            queue.addLast(child);
                        }
                    }
                } finally {
                    if (node != root) {
                        //noinspection deprecation
                        node.recycle();
                    }
                }
            }
        } finally {
            while (!queue.isEmpty()) {
                AccessibilityNodeInfo node = queue.removeFirst();
                if (node != root) {
                    //noinspection deprecation
                    node.recycle();
                }
            }
        }
        return false;
    }
}
