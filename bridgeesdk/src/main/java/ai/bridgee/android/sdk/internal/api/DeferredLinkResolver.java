package ai.bridgee.android.sdk.internal.api;

import android.os.Handler;
import android.os.Looper;
import ai.bridgee.android.sdk.DeferredDestination;
import ai.bridgee.android.sdk.ResponseCallback;
import org.json.JSONObject;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DeferredLinkResolver {
    private static final String CONTENT_TYPE = "application/vnd.bridgee.deferred+json";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private DeferredLinkResolver() {}

    static URI https(String value) {
        try {
            if (value == null || value.length() > 2048) return null;
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getRawUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443) ? uri : null;
        } catch (IllegalArgumentException e) { return null; }
    }
    private static boolean sameOrigin(URI a, URI b) {
        return a.getHost().equalsIgnoreCase(b.getHost()); // Both already HTTPS/default port.
    }
    public static boolean allowedSource(String value, String[] prefixes) {
        URI link = https(value);
        if (link == null || link.getRawQuery() != null || link.getRawFragment() != null || prefixes == null) return false;
        for (String prefix : prefixes) {
            URI parsed = https(prefix);
            if (parsed != null && parsed.getRawPath().endsWith("/") && parsed.getRawQuery() == null
                    && parsed.getRawFragment() == null && sameOrigin(link, parsed)
                    && link.getRawPath().startsWith(parsed.getRawPath()) && link.normalize().equals(link)) return true;
        }
        return false;
    }
    public static boolean allowedDestination(String value, String[] origins) {
        URI link = https(value);
        if (link == null || origins == null) return false;
        for (String origin : origins) {
            URI parsed = https(origin);
            if (parsed != null && (parsed.getRawPath().isEmpty() || parsed.getRawPath().equals("/"))
                    && parsed.getRawQuery() == null && parsed.getRawFragment() == null && sameOrigin(link, parsed)) return true;
        }
        return false;
    }
    public static String fromReferrer(String raw) {
        if (raw == null || !raw.startsWith("success:") || raw.length() > 4104) return null;
        try {
            String result = null;
            int count = 0;
            for (String pair : raw.substring(8).split("&")) {
                String[] parts = pair.split("=", 2);
                if ("bridgee_link".equals(URLDecoder.decode(parts[0], "UTF-8"))) {
                    count++;
                    result = parts.length == 2 ? URLDecoder.decode(parts[1], "UTF-8") : "";
                }
            }
            return count == 1 ? result : null;
        } catch (Exception e) { return null; }
    }
    public static void resolve(String source, String[] prefixes, String[] origins, ResponseCallback<DeferredDestination> callback) {
        // Copy caller-owned arrays before background work.
        final String[] allowedOrigins = origins == null ? new String[0] : origins.clone();
        if (!allowedSource(source, prefixes) || allowedOrigins.length == 0) { callback.ok(null); return; }
        WORKER.execute(() -> {
            DeferredDestination result = null;
            HttpURLConnection connection = null;
            try {
                URL endpoint = new URL("https://api.bridgee.ai/deferred?platform=android&link=" + URLEncoder.encode(source, "UTF-8"));
                connection = (HttpURLConnection) endpoint.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(2000); connection.setReadTimeout(4000);
                connection.setRequestProperty("Accept", CONTENT_TYPE);
                String type = connection.getContentType();
                if (connection.getResponseCode() == 200 && type != null && CONTENT_TYPE.equalsIgnoreCase(type.split(";", 2)[0].trim())) {
                    ByteArrayOutputStream body = new ByteArrayOutputStream();
                    try (InputStream input = connection.getInputStream()) {
                        byte[] bytes = new byte[1024]; int count;
                        long deadline = System.nanoTime() + 5_000_000_000L;
                        while ((count = input.read(bytes)) != -1) {
                            if (body.size() + count > 8192 || System.nanoTime() > deadline) throw new java.io.IOException("deferred_response_limit");
                            body.write(bytes, 0, count);
                        }
                    }
                    JSONObject payload = new JSONObject(new String(body.toByteArray(), StandardCharsets.UTF_8));
                    String destination = payload.optString("appLink", "");
                    if (source.equals(payload.optString("link")) && !source.equals(destination) && allowedDestination(destination, allowedOrigins))
                        result = new DeferredDestination(source, destination);
                }
            } catch (Exception ignored) {
                // Unavailable/offline is retriable. Never persist it as consumed.
            } finally { if (connection != null) connection.disconnect(); }
            final DeferredDestination value = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.ok(value));
        });
    }
}
