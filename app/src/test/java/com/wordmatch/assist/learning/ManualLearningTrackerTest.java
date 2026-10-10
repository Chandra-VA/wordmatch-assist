package com.wordmatch.assist.learning;

import com.wordmatch.assist.model.WordBox;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class ManualLearningTrackerTest {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1600;

    @Test
    public void rapidNextPairCannotReuseThePreviousRightTile() {
        WordBox museum = box("美术馆", 100, 300);
        WordBox gallery = box("gallery", 700, 300);
        WordBox railway = box("铁路", 100, 400);
        WordBox railroad = box("railroad", 700, 400);
        List<WordBox> board = Arrays.asList(museum, gallery, railway, railroad);
        ManualLearningTracker tracker = new ManualLearningTracker();

        tracker.recordClick(museum, board, WIDTH, 100L);
        tracker.recordClick(gallery, board, WIDTH, 120L);
        tracker.recordClick(railway, board, WIDTH, 140L);

        assertEquals(1, tracker.getPendingAttempts().size());
        assertEquals(museum, tracker.getPendingAttempts().get(0).getLeft());
        assertEquals(gallery, tracker.getPendingAttempts().get(0).getRight());
        assertEquals(railway, tracker.getActiveLeft());
        assertNull(tracker.getActiveRight());

        tracker.recordClick(railroad, board, WIDTH, 160L);

        assertEquals(2, tracker.getPendingAttempts().size());
        assertEquals(railway, tracker.getPendingAttempts().get(1).getLeft());
        assertEquals(railroad, tracker.getPendingAttempts().get(1).getRight());
    }

    @Test
    public void acceptsTwoTargetsThatDisappearTogether() {
        WordBox museum = box("美术馆", 100, 300);
        WordBox gallery = box("gallery", 700, 300);
        WordBox railway = box("铁路", 100, 400);
        WordBox railroad = box("railroad", 700, 400);
        List<WordBox> board = Arrays.asList(museum, gallery, railway, railroad);
        ManualLearningTracker tracker = new ManualLearningTracker();
        tracker.recordClick(museum, board, WIDTH, 100L);
        tracker.recordClick(gallery, board, WIDTH, 120L);

        assertEquals(
                ManualLearningTracker.Observation.COMPLETED,
                tracker.getPendingAttempts().get(0).observe(
                        Arrays.asList(railway, railroad),
                        WIDTH,
                        HEIGHT,
                        200L
                )
        );
    }

    @Test
    public void rejectsTargetsThatDisappearInSeparateMatches() {
        WordBox museum = box("美术馆", 100, 300);
        WordBox wrongRight = box("railroad", 700, 400);
        WordBox gallery = box("gallery", 700, 300);
        WordBox railway = box("铁路", 100, 400);
        List<WordBox> board = Arrays.asList(museum, gallery, railway, wrongRight);
        ManualLearningTracker tracker = new ManualLearningTracker();
        tracker.recordClick(museum, board, WIDTH, 100L);
        tracker.recordClick(wrongRight, board, WIDTH, 120L);
        ManualLearningTracker.Attempt attempt = tracker.getPendingAttempts().get(0);

        assertEquals(
                ManualLearningTracker.Observation.WAITING,
                attempt.observe(Arrays.asList(gallery, railway, wrongRight), WIDTH, HEIGHT, 200L)
        );
        assertEquals(
                ManualLearningTracker.Observation.REJECTED,
                attempt.observe(Arrays.asList(gallery, railway), WIDTH, HEIGHT, 500L)
        );
    }

    private static WordBox box(String text, int x, int y) {
        return new WordBox(text, x, y, x + 120, y + 50);
    }
}
