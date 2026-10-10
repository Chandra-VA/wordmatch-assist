package com.wordmatch.assist.ai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Small dependency-free client for supported Chat Completions providers. */
public final class AiMatchClient {
    private static final int MAX_RESPONSE_CHARS = 65_536;
    private static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ai-request-deadline");
        thread.setDaemon(true);
        return thread;
    });
    interface Connections { HttpURLConnection open(URL url) throws IOException; }
    private final Connections connections;
    public AiMatchClient() { this(url -> (HttpURLConnection) url.openConnection()); }
    AiMatchClient(Connections connections) { this.connections = connections; }

    public static final class Call {
        private volatile boolean cancelled;
        private volatile boolean timedOut;
        private HttpURLConnection connection;
        public void cancel() { stop(false); }
        private void stop(boolean timeout) {
            HttpURLConnection active;
            synchronized (this) { cancelled = true; timedOut |= timeout; active = connection; }
            if (active != null) active.disconnect();
        }
        private synchronized void attach(HttpURLConnection value) throws IOException {
            if (cancelled) { value.disconnect(); throw new IOException("请求已取消"); }
            connection = value;
        }
        private synchronized void detach() { connection = null; }
        private void check() throws IOException {
            if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("请求已取消");
        }
    }

    public Result match(
            AiMatchSettings.Settings settings,
            List<String> visibleLeft,
            List<String> visibleRight
    ) {
        return match(settings, visibleLeft, visibleRight, new Call());
    }

    public Result match(AiMatchSettings.Settings settings, List<String> visibleLeft,
                        List<String> visibleRight, Call call) {
        if (settings == null || !settings.isConfigured()) {
            return Result.error("大模型设置不完整");
        }
        HttpURLConnection connection = null;
        ScheduledFuture<?> deadline = null;
        try {
            call.check();
            deadline = DEADLINES.schedule(() -> call.stop(true), settings.getTimeoutMs(), TimeUnit.MILLISECONDS);
            connection = connections.open(new URL(settings.getEndpoint()));
            call.attach(connection);
            // Never forward the Authorization header to a redirected destination.
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(settings.getTimeoutMs());
            connection.setReadTimeout(settings.getTimeoutMs());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "WordMatchAssist/3.2.15");
            if (settings.hasApiKey()) {
                connection.setRequestProperty("Authorization", "Bearer " + settings.getApiKey());
            }

            byte[] request = buildRequest(
                    settings.getProvider(),
                    settings.getModel(),
                    visibleLeft,
                    visibleRight
            )
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(request.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(request);
            }

            int status = connection.getResponseCode();
            InputStream responseStream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = readLimited(responseStream, call);
            call.check();
            if (status < 200 || status >= 300) {
                return Result.error(buildHttpError(status,
                        AiTransportPolicy.redact(response, settings.getApiKey())));
            }
            List<AiPairValidator.CandidatePair> candidates = parseCandidates(response);
            List<AiPairValidator.ValidatedPair> validated = AiPairValidator.validate(
                    visibleLeft,
                    visibleRight,
                    candidates
            );
            if (validated.isEmpty()) {
                return Result.error("模型没有返回可验证的高置信配对");
            }
            return Result.success(validated);
        } catch (IOException error) {
            if (call.timedOut) return Result.error("大模型请求超时（" + settings.getTimeoutMs() + "ms）");
            if (call.cancelled) return Result.error("大模型请求已取消");
            return Result.error("网络请求失败：" + safeMessage(new IOException(
                    AiTransportPolicy.redact(error.getMessage(), settings.getApiKey()))));
        } catch (JSONException error) {
            return Result.error("模型返回格式无效");
        } finally {
            if (deadline != null) deadline.cancel(false);
            call.detach();
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject buildRequest(
            AiProvider provider,
            String model,
            List<String> visibleLeft,
            List<String> visibleRight
    ) throws JSONException {
        String system = "You solve bilingual word-card matching. Pair only clear translations. "
                + "Use each input at most once, copy input strings exactly, and omit uncertain pairs. "
                + "Return JSON only: {\"pairs\":[{\"left\":\"...\",\"right\":\"...\","
                + "\"confidence\":0.0}]} where confidence is 0 to 1.";
        JSONObject board = new JSONObject()
                .put("left", new JSONArray(visibleLeft))
                .put("right", new JSONArray(visibleRight));
        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", system))
                .put(new JSONObject()
                        .put("role", "user")
                        .put("content", "Match this board: " + board));
        JSONObject request = new JSONObject()
                .put("model", model)
                .put("messages", messages);
        if (provider.usesJsonResponseFormat()) {
            request.put("response_format", new JSONObject().put("type", "json_object"));
        }
        return applyProviderParameters(request, provider);
    }

    private JSONObject applyProviderParameters(JSONObject request, AiProvider provider)
            throws JSONException {
        if (provider.usesQwenThinkingParameter()) {
            request.put("enable_thinking", false);
        } else if (provider.usesDeepSeekThinkingParameter()) {
            request.put("thinking", new JSONObject().put("type", "disabled"));
        }
        return request;
    }

    private String buildHttpError(int status, String response) {
        String detail = extractErrorDetail(response);
        return detail.isEmpty()
                ? "接口返回 HTTP " + status
                : "接口返回 HTTP " + status + "：" + detail;
    }

    private String extractErrorDetail(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }
        String detail = "";
        try {
            JSONObject envelope = new JSONObject(response);
            JSONObject error = envelope.optJSONObject("error");
            if (error != null) {
                detail = error.optString("message", "");
                if (detail.isEmpty()) {
                    detail = error.optString("code", "");
                }
            }
            if (detail.isEmpty()) {
                detail = envelope.optString("message", "");
            }
            if (detail.isEmpty()) {
                detail = envelope.optString("code", "");
            }
        } catch (JSONException ignored) {
            detail = response;
        }
        String clean = detail.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() > 180 ? clean.substring(0, 180) : clean;
    }

    private List<AiPairValidator.CandidatePair> parseCandidates(String response)
            throws JSONException {
        JSONObject envelope = new JSONObject(response);
        JSONArray choices = envelope.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return Collections.emptyList();
        }
        JSONObject message = choices.getJSONObject(0).optJSONObject("message");
        if (message == null) {
            return Collections.emptyList();
        }
        String content = readMessageContent(message);
        int objectStart = content.indexOf('{');
        int objectEnd = content.lastIndexOf('}');
        if (objectStart < 0 || objectEnd <= objectStart) {
            return Collections.emptyList();
        }
        JSONObject payload = new JSONObject(content.substring(objectStart, objectEnd + 1));
        JSONArray pairs = payload.optJSONArray("pairs");
        if (pairs == null) {
            return Collections.emptyList();
        }
        List<AiPairValidator.CandidatePair> output = new ArrayList<>();
        for (int index = 0; index < pairs.length(); index++) {
            JSONObject pair = pairs.optJSONObject(index);
            if (pair == null) {
                continue;
            }
            output.add(new AiPairValidator.CandidatePair(
                    pair.optString("left", ""),
                    pair.optString("right", ""),
                    pair.optDouble("confidence", 0.0)
            ));
        }
        return output;
    }

    private String readMessageContent(JSONObject message) {
        Object value = message.opt("content");
        if (value instanceof String) {
            return ((String) value).trim();
        }
        if (!(value instanceof JSONArray)) {
            return "";
        }
        StringBuilder output = new StringBuilder();
        JSONArray parts = (JSONArray) value;
        for (int index = 0; index < parts.length(); index++) {
            JSONObject part = parts.optJSONObject(index);
            if (part == null) {
                continue;
            }
            String text = part.optString("text", "");
            if (text.isEmpty()) {
                text = part.optString("content", "");
            }
            output.append(text);
        }
        return output.toString().trim();
    }

    private String readLimited(InputStream input, Call call) throws IOException {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            char[] buffer = new char[2048];
            int count;
            while ((count = reader.read(buffer)) >= 0) {
                call.check();
                if (builder.length() + count > MAX_RESPONSE_CHARS) throw new IOException("模型响应过大");
                builder.append(buffer, 0, count);
            }
        }
        return builder.toString();
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        String clean = message.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() > 80 ? clean.substring(0, 80) : clean;
    }

    public static final class Result {
        private final List<AiPairValidator.ValidatedPair> pairs;
        private final String error;

        private Result(List<AiPairValidator.ValidatedPair> pairs, String error) {
            this.pairs = pairs;
            this.error = error;
        }

        public static Result success(List<AiPairValidator.ValidatedPair> pairs) {
            return new Result(new ArrayList<>(pairs), "");
        }

        public static Result error(String error) {
            return new Result(Collections.emptyList(), error == null ? "未知错误" : error);
        }

        public boolean isSuccess() {
            return !pairs.isEmpty();
        }

        public List<AiPairValidator.ValidatedPair> getPairs() {
            return new ArrayList<>(pairs);
        }

        public String getError() {
            return error;
        }
    }
}
