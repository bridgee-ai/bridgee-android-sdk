package ai.bridgee.android.sdk.internal.api;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeferredLinkResolverTest {
    private final String link = "https://go.bridgee.app/tenda/offer";
    private final String[] prefixes = {"https://go.bridgee.app/tenda/"};
    @Test public void requiresExactOriginAndOrganizationBoundary() {
        assertTrue(DeferredLinkResolver.allowedSource(link, prefixes));
        for (String value : new String[]{"https://go.bridgee.app/tenda-other/offer", "https://other.example/tenda/offer",
                link+"?x=1", link+"#x", "http://go.bridgee.app/tenda/offer", "https://user@go.bridgee.app/tenda/offer",
                "https://go.bridgee.app:444/tenda/offer", "https://go.bridgee.app/tenda/../foreign/offer"})
            assertFalse(value, DeferredLinkResolver.allowedSource(value, prefixes));
    }
    @Test public void onlyAcceptsAppOwnedHttpsOrigins() {
        String[] origins = {"https://app.example.com"};
        assertTrue(DeferredLinkResolver.allowedDestination("https://app.example.com/offer/123", origins));
        for (String value : new String[]{"https://app.example.com.evil.test/offer", "javascript:alert(1)",
                "https://user@app.example.com/offer", "https://app.example.com:444/offer"})
            assertFalse(DeferredLinkResolver.allowedDestination(value, origins));
        assertFalse(DeferredLinkResolver.allowedDestination("https://app.example.com/offer", new String[]{"https://app.example.com/path"}));
    }
    @Test public void decodesExactlyOneBridgeeLinkAndRejectsAmbiguousOrFailedReferrer() {
        assertEquals(link, DeferredLinkResolver.fromReferrer("success:utm_source=google&bridgee_link=https%3A%2F%2Fgo.bridgee.app%2Ftenda%2Foffer"));
        for (String raw : new String[]{"error:timeout", "success:utm_source=google", "success:bridgee_link=a&bridgee_link=b", "success:bridgee_link=%GG"})
            assertNull(DeferredLinkResolver.fromReferrer(raw));
    }
}
