package ai.bridgee.android.sdk.internal.model;

import android.os.Bundle;

public class MatchResponse {
    private final String utmSource;
    private final String utmMedium;
    private final String utmCampaign;

    public MatchResponse(String utmSource, String utmMedium, String utmCampaign) {
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
    }

    public String getUtmSource() { return utmSource; }
    public String getUtmMedium() { return utmMedium; }
    public String getUtmCampaign() { return utmCampaign; }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (utmSource != null) { 
            bundle.putString("utm_source", utmSource); 
            bundle.putString("source", utmSource); 
        }
        if (utmMedium != null) { 
            bundle.putString("utm_medium", utmMedium); 
            bundle.putString("medium", utmMedium); 
        }
        if (utmCampaign != null) { 
            bundle.putString("utm_campaign", utmCampaign); 
            bundle.putString("campaign", utmCampaign); 
        }
        return bundle;
    }
}
