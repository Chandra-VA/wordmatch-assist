package com.wordmatch.assist.ai;

import org.json.*;
import org.junit.Test;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public final class AiMatchClientTest {
    private static final List<String> LEFT = Arrays.asList("苹果", "车费");
    private static final List<String> RIGHT = Arrays.asList("fare", "apple");
    private static final String PAIRS = "{\"pairs\":[{\"left\":\"苹果\",\"right\":\"apple\",\"confidence\":0.99},{\"left\":\"车费\",\"right\":\"fare\",\"confidence\":0.98}]}";
    private AiMatchSettings.Settings settings(String provider) {
        return AiMatchSettings.createTransient(provider,"https://unit.test/chat/completions","unit-model","test-secret",1000);
    }
    private String response(Object content) throws Exception {
        return new JSONObject().put("choices",new JSONArray().put(new JSONObject().put("message",new JSONObject().put("content",content)))).toString();
    }
    @Test public void realJsonRequestAndShuffledResponseRoundTrip() throws Exception {
        Fake connection = new Fake(200,response(PAIRS));
        AiMatchClient.Result result = new AiMatchClient(url -> connection).match(settings("qwen"),LEFT,RIGHT);
        assertTrue(result.getError(),result.isSuccess());
        assertEquals(2,result.getPairs().size());
        assertEquals("apple",result.getPairs().get(0).getRight());
        com.wordmatch.assist.matching.PairMatcher matcher = new com.wordmatch.assist.matching.PairMatcher((a,b) -> {
            for (AiPairValidator.ValidatedPair pair : result.getPairs()) {
                if (pair.getLeft().equals(a) && pair.getRight().equals(b)) return 1.0;
            }
            return 0.0;
        });
        List<com.wordmatch.assist.model.WordBox> words = Arrays.asList(
                new com.wordmatch.assist.model.WordBox("苹果",10,200,80,240),
                new com.wordmatch.assist.model.WordBox("车费",10,300,80,340),
                new com.wordmatch.assist.model.WordBox("fare",700,200,780,240),
                new com.wordmatch.assist.model.WordBox("apple",700,300,780,340));
        assertEquals(2, matcher.match(words,1000).size());
        JSONObject request = new JSONObject(connection.sent.toString("UTF-8"));
        assertFalse(request.getBoolean("enable_thinking"));
        assertEquals("json_object",request.getJSONObject("response_format").getString("type"));
        assertEquals("Bearer test-secret",connection.getRequestProperty("Authorization"));
        assertFalse(connection.getInstanceFollowRedirects());
        assertTrue(connection.disconnected);
    }
    @Test public void deepseekAndGenericGatewayParametersRemainCompatible() throws Exception {
        for (String provider : Arrays.asList("deepseek","compatible")) {
            Fake connection = new Fake(200,response("```json\n"+PAIRS+"\n```"));
            assertTrue(new AiMatchClient(url -> connection).match(settings(provider),LEFT,RIGHT).isSuccess());
            JSONObject request = new JSONObject(connection.sent.toString("UTF-8"));
            if (provider.equals("deepseek")) assertEquals("disabled",request.getJSONObject("thinking").getString("type"));
            else { assertFalse(request.has("thinking")); assertFalse(request.has("response_format")); }
        }
    }
    @Test public void contentBlocksAreParsedButMalformedAndUntrustedPairsAreRejected() throws Exception {
        Fake blocks = new Fake(200,response(new JSONArray().put(new JSONObject().put("text",PAIRS))));
        assertTrue(new AiMatchClient(url -> blocks).match(settings("compatible"),LEFT,RIGHT).isSuccess());
        for (String payload : Arrays.asList("not json", "{\"pairs\":[{\"left\":\"不存在\",\"right\":\"apple\",\"confidence\":1}]}")) {
            Fake invalid = new Fake(200,response(payload));
            assertFalse(new AiMatchClient(url -> invalid).match(settings("compatible"),LEFT,RIGHT).isSuccess());
        }
    }
    @Test public void httpErrorsRedactCredentialsAndNeverBecomeSuccess() throws Exception {
        Fake error = new Fake(401,"{\"error\":{\"message\":\"bad test-secret Bearer hidden-token\"}}");
        String detail = new AiMatchClient(url -> error).match(settings("compatible"),LEFT,RIGHT).getError();
        assertTrue(detail.contains("401"));
        assertFalse(detail.contains("test-secret"));
        assertFalse(detail.contains("hidden-token"));
        Fake redirect = new Fake(302,"");
        assertTrue(new AiMatchClient(url -> redirect).match(settings("compatible"),LEFT,RIGHT).getError().contains("302"));
    }
    @Test public void cancelledCallNeverOpensConnection() {
        AiMatchClient.Call call = new AiMatchClient.Call(); call.cancel();
        AiMatchClient.Result result = new AiMatchClient(url -> { throw new AssertionError("opened cancelled request"); })
                .match(settings("compatible"),LEFT,RIGHT,call);
        assertTrue(result.getError().contains("取消"));
    }
    @Test public void cancellationDisconnectsBlockedRequestAndFreesWorker() throws Exception {
        Fake blocked = new Fake(200,""); blocked.block = true;
        AiMatchClient.Call call = new AiMatchClient.Call();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<AiMatchClient.Result> result = executor.submit(() -> new AiMatchClient(url -> blocked).match(settings("compatible"),LEFT,RIGHT,call));
            assertTrue(blocked.entered.await(1,TimeUnit.SECONDS));
            call.cancel();
            assertTrue(result.get(1,TimeUnit.SECONDS).getError().contains("取消"));
            assertTrue(blocked.disconnected);
            assertEquals("next",executor.submit(() -> "next").get(1,TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
    }
    @Test public void wallClockDeadlineDisconnectsAnUnresponsiveServer() throws Exception {
        Fake blocked = new Fake(200,""); blocked.block = true;
        long started = System.nanoTime();
        AiMatchClient.Result result = new AiMatchClient(url -> blocked).match(settings("compatible"),LEFT,RIGHT);
        assertTrue(result.getError(), result.getError().contains("超时"));
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started) < 2500);
        assertTrue(blocked.disconnected);
    }
    private static final class Fake extends HttpURLConnection {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final byte[] body; final int status;
        volatile boolean disconnected, block;
        final CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        Fake(int status,String body) throws Exception { super(new URL("https://unit.test/chat/completions")); this.status=status; this.body=body.getBytes(StandardCharsets.UTF_8); }
        public void connect() {}
        public boolean usingProxy() { return false; }
        public void disconnect() { disconnected=true; released.countDown(); }
        public OutputStream getOutputStream() { return sent; }
        public int getResponseCode() throws IOException {
            if (block) { entered.countDown(); try { released.await(3,TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } throw new IOException("disconnected"); }
            return status;
        }
        public InputStream getInputStream() { return new ByteArrayInputStream(body); }
        public InputStream getErrorStream() { return getInputStream(); }
    }
}
