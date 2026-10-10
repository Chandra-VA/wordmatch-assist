package com.wordmatch.assist.matching;

import com.wordmatch.assist.model.MatchPair;
import com.wordmatch.assist.model.WordBox;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public final class VisualBoardSelectionTest {
    private static final WordBox BOOTS = new WordBox("靴子",185,25,219,46);
    private static final WordBox BOOT = new WordBox("boot",580,25,616,46);
    private static final WordBox GROUP = new WordBox("团",193,189,211,210);
    private static final WordBox TOUR = new WordBox("tour",581,268,617,289);
    private static final List<WordBox> WORDS = Arrays.asList(BOOTS,BOOT,GROUP,TOUR,
            new WordBox("发送",185,107,219,128),new WordBox("send",580,107,616,128),
            new WordBox("穿过",185,269,219,290),new WordBox("cross",578,189,620,210),
            new WordBox("总是",185,349,219,370),new WordBox("always",575,349,623,370));
    private static final List<WordBox> CARDS = Arrays.asList(new WordBox("",15,2,389,67),
            new WordBox("",411,2,785,67),new WordBox("",15,164,389,231),new WordBox("",411,245,785,311),
            new WordBox("",15,83,389,149),new WordBox("",411,83,785,149),
            new WordBox("",15,245,389,311),new WordBox("",411,164,785,231),
            new WordBox("",15,327,389,393),new WordBox("",411,327,785,393));

    private GreenCardDetector.Pixels selectedGroup() {
        return (x,y) -> x < 389 && y >= 164 && y < 231 ? 0xFFDDF4FF : 0xFFFFFFFF;
    }

    @Test public void recordingRedirectsBootsWaitToAlreadySelectedGroupAndOnlyItsPartner() throws Exception {
        byte[] rgb;
        try (InputStream input = new GZIPInputStream(getClass().getResourceAsStream("/unrelated-selection-board.rgb.gz"))) {
            rgb = input.readAllBytes();
        }
        assertEquals(800 * 396 * 3, rgb.length);
        GreenCardDetector.Pixels pixels = (x,y) -> {
            int i = (y * 800 + x) * 3;
            return 0xFF000000 | (rgb[i]&255)<<16 | (rgb[i+1]&255)<<8 | (rgb[i+2]&255);
        };
        VisualBoardSelection visual = new VisualBoardSelection();
        visual.prepare(WORDS,CARDS,800,396,1000);
        visual.observe(pixels,800,396,1000);
        assertTrue(visual.selectedWords(1000).isEmpty());
        visual.observe(pixels,800,396,1048);
        List<WordBox> selected = visual.selectedWords(1048);
        assertEquals(Collections.singletonList(GROUP), selected);
        MatchPair old = new MatchPair(BOOTS,BOOT,1,1), current = new MatchPair(GROUP,TOUR,1,3);
        ActivePairSelectionGuard guard = new ActivePairSelectionGuard();
        assertFalse(guard.observe(selected,BOOTS,BOOT,800,1048));
        assertTrue(guard.observe(selected,BOOTS,BOOT,800,1096));
        List<MatchPair> pairs = Arrays.asList(old,current);
        AutoPairRecovery recovery = new AutoPairRecovery();
        recovery.recordFailure(old,800,396,1096);
        recovery.reviewObservedSelectionNow(1096);
        assertEquals(AutoPairRecovery.Action.RESUME,
                recovery.evaluate(pairs,selected,true,true,800,396,1096));
        SelectedPairPlanner.Plan plan = SelectedPairPlanner.choose(pairs,selected,800,396);
        assertNotNull(plan);
        assertTrue(plan.isFirstAlreadySelected());
        assertEquals(TOUR,plan.getPair().getSecond());
        assertEquals(GROUP,plan.getPair().getFirst());
    }

    @Test public void whiteGreenAndColoredWordBadgesDoNotSelectAnotherCard() {
        VisualBoardSelection visual = prepared();
        GreenCardDetector.Pixels badge = (x,y) -> x >= 180 && x < 225 && y >= 20 && y < 48
                ? 0xFFDDF4FF : 0xFFFFFFFF;
        visual.observe(badge,800,396,1000);
        visual.observe(badge,800,396,1048);
        assertTrue(visual.selectedWords(1048).isEmpty());
        visual.observe((x,y)->0xFFD7FFB8,800,396,1100);
        visual.observe((x,y)->0xFFD7FFB8,800,396,1148);
        assertTrue(visual.selectedWords(1148).isEmpty());
    }

    @Test public void expiredPixelsAndStaleNodeGeometryCannotAuthorizeSelection() {
        VisualBoardSelection visual = prepared();
        visual.observe(selectedGroup(),800,396,1000);
        visual.observe(selectedGroup(),800,396,1048);
        assertFalse(visual.selectedWords(1168).isEmpty());
        assertTrue(visual.selectedWords(1169).isEmpty());
        visual.observe(selectedGroup(),800,396,1300);
        assertTrue(visual.selectedWords(1300).isEmpty());
    }

    @Test public void replacementRotationMissingControlsAndResetInvalidateEarlierBlueCard() {
        VisualBoardSelection visual = prepared();
        visual.observe(selectedGroup(),800,396,1000);
        visual.observe(selectedGroup(),800,396,1048);
        List<WordBox> replacement = new ArrayList<>(WORDS);
        replacement.set(2,new WordBox("新词",193,189,211,210));
        visual.prepare(replacement,CARDS,800,396,1050);
        assertTrue(visual.selectedWords(1050).isEmpty());
        visual.observe(selectedGroup(),396,800,1100);
        assertTrue(visual.selectedWords(1148).isEmpty());
        List<WordBox> missing = new ArrayList<>(CARDS);
        missing.set(0,null);
        visual.prepare(WORDS,missing,800,396,1200);
        visual.observe(selectedGroup(),800,396,1200);
        visual.observe(selectedGroup(),800,396,1248);
        assertTrue(visual.selectedWords(1248).isEmpty());
        visual.reset();
        assertTrue(visual.selectedWords(1248).isEmpty());
    }

    @Test public void twoBlueCardsArePreservedSoThePlannerCannotStartAnotherPair() {
        VisualBoardSelection visual = prepared();
        visual.observe((x,y)->0xFFDDF4FF,800,396,1000);
        visual.observe((x,y)->0xFFDDF4FF,800,396,1048);
        assertEquals(WORDS.size(),visual.selectedWords(1048).size());
        assertNull(SelectedPairPlanner.choose(Collections.singletonList(new MatchPair(BOOTS,BOOT,1,1)),
                visual.selectedWords(1048),800,396));
    }

    @Test public void screenshotFallbackRequiresRepeatedObservationsAndCurrentBoardIdentity() {
        VisualBoardSelection visual = prepared();
        List<WordBox> group = Collections.singletonList(GROUP);
        visual.observeSelected(WORDS,CARDS,800,396,group,1000);
        assertTrue(visual.selectedWords(1000).isEmpty());
        visual.prepare(WORDS,CARDS,800,396,1350);
        visual.observeSelected(WORDS,CARDS,800,396,group,1350);
        assertEquals(group,visual.selectedWords(1350));
        List<WordBox> moved = new ArrayList<>(CARDS);
        moved.set(2,new WordBox("",15,174,389,241));
        visual.prepare(WORDS,moved,800,396,1360);
        visual.observeSelected(WORDS,CARDS,800,396,group,1400);
        assertTrue(visual.selectedWords(1400).isEmpty());
    }

    @Test public void aBlueCardTurningWhiteClearsTheOldSelectionImmediately() {
        VisualBoardSelection visual = prepared();
        visual.observe(selectedGroup(),800,396,1000);
        visual.observe(selectedGroup(),800,396,1048);
        visual.observe((x,y)->0xFFFFFFFF,800,396,1064);
        assertTrue(visual.selectedWords(1064).isEmpty());
    }

    private VisualBoardSelection prepared() {
        VisualBoardSelection visual = new VisualBoardSelection();
        visual.prepare(WORDS,CARDS,800,396,1000);
        return visual;
    }
}
