package com.wordmatch.assist.ai;
import com.wordmatch.assist.model.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AiBoardRequestTest {
    private WordBox word(String s, int x, int y) { return new WordBox(s, x, y, x + 80, y + 30); }
    @Test public void prefetchOmitsKnownPairsAndLeavesUnknownBoard() {
        WordBox a = word("苹果", 10, 200), b = word("apple", 700, 200);
        AiBoardRequest request = AiBoardRequest.from(Arrays.asList(a,b,word("车费",10,300),word("fare",700,300),
                word("类似",10,400),word("similar",700,400)), Collections.singletonList(new MatchPair(a,b,1,1)),1000);
        assertEquals(Arrays.asList("车费","类似"), request.left);
        assertEquals(Arrays.asList("fare","similar"), request.right);
        assertTrue(request.needsModel());
    }
    @Test public void lastPairNeedsNoNetworkAndDuplicatesRemainVisibleToValidator() {
        assertFalse(AiBoardRequest.from(Arrays.asList(word("x",10,200),word("y",700,200)),Collections.emptyList(),1000).needsModel());
        AiBoardRequest request = AiBoardRequest.from(Arrays.asList(word("x",10,200),word("x",10,300),
                word("y",700,200),word("z",700,300)),Collections.emptyList(),1000);
        assertEquals(Arrays.asList("x","x"), request.left);
        assertTrue(request.needsModel());
    }
}
