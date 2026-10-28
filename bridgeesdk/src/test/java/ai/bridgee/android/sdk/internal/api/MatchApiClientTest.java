package ai.bridgee.android.sdk.internal.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class MatchApiClientTest {
    static class Response extends HttpURLConnection {
        final int code;
        final String body;
        boolean read;
        Response(int code, String body) throws Exception {
            super(new URL("https://example.invalid/match"));
            this.code = code;
            this.body = body;
        }
        public int getResponseCode() { return code; }
        public InputStream getInputStream() {
            read = true;
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }
        public void disconnect() {}
        public boolean usingProxy() { return false; }
        public void connect() {}
    }

    @Test public void redirectsAreNeverFollowedWithTenantCredentials() throws Exception {
        Response response = new Response(302, "sensitive-error-body");
        MatchApiClient.configureConnection(response);
        assertFalse(response.getInstanceFollowRedirects());
        assertEquals("POST", response.getRequestMethod());
        try {
            MatchApiClient.readResponse(response);
            fail("Redirect must fail");
        } catch (IOException error) {
            assertEquals("match_http_error:302", error.getMessage());
            assertFalse(response.read);
        }
    }

    @Test public void errorPayloadIsNotReadOrExposedToCallbacks() throws Exception {
        Response response = new Response(500, "tenant-key-and-private-referrer");
        try {
            MatchApiClient.readResponse(response);
            fail("Server error expected");
        } catch (IOException error) {
            assertEquals("match_http_error:500", error.getMessage());
            assertFalse(response.read);
        }
    }

    @Test public void oversizedSuccessfulResponseIsBounded() throws Exception {
        char[] data = new char[17000];
        java.util.Arrays.fill(data, 'a');
        try {
            MatchApiClient.readResponse(new Response(200, new String(data)));
            fail("Oversized response expected");
        } catch (IOException error) {
            assertEquals("match_response_too_large", error.getMessage());
        }
    }

    @Test public void invalidJsonDoesNotExposeResponseContents() throws Exception {
        try {
            MatchApiClient.readResponse(new Response(200, "private-referrer-invalid-json"));
            fail("Invalid JSON expected");
        } catch (org.json.JSONException error) {
            assertEquals("match_invalid_response", error.getMessage());
        }
    }

    @Test public void campaignCaseAndEncodingArePreserved() throws Exception {
        assertEquals("Launch+Summer&Q4", MatchApiClient.readResponse(
            new Response(200, "{\"utm_campaign\":\"Launch+Summer&Q4\"}")).getString("utm_campaign"));
    }
}
