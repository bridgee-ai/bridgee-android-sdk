package ai.bridgee.android.sdk;

import android.os.Bundle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class AttributionDeliveryTest {
    static class RecordingProvider implements AnalyticsProvider {
        final List<String> events = new ArrayList<>();
        final Map<String, String> properties = new HashMap<>();
        Bundle lastParams;

        public void setUserProperty(String name, String value) { properties.put(name, value); }
        public void logEvent(String name, Bundle params) {
            events.add(name);
            lastParams = params;
        }
    }

    @Test
    public void keepsCampaignAndPropertiesWithoutEmittingFirstOpenOrPurchase() {
        RecordingProvider provider = new RecordingProvider();
        BridgeeSDK sdk = new BridgeeSDK(RuntimeEnvironment.getApplication(), provider,
            "tenant-test", "test-only", false);
        sdk.deliverAttribution(new MatchResponse("TikTok", "paid_social", "Launch+Summer"));
        assertEquals(java.util.Arrays.asList("tenant_test_campaign_details", "campaign_details"), provider.events);
        assertEquals("TikTok", provider.properties.get("install_source"));
        assertEquals("paid_social", provider.properties.get("install_medium"));
        assertEquals("Launch+Summer", provider.properties.get("install_campaign"));
        assertEquals("Launch+Summer", provider.lastParams.getString("campaign"));
    }

    @Test
    public void dryRunDoesNotDeliverAnalytics() {
        RecordingProvider provider = new RecordingProvider();
        BridgeeSDK sdk = new BridgeeSDK(RuntimeEnvironment.getApplication(), provider,
            "tenant-test", "test-only", true);
        sdk.deliverAttribution(new MatchResponse("TikTok", "paid_social", "Launch"));
        assertTrue(provider.events.isEmpty());
        assertTrue(provider.properties.isEmpty());
    }
}
