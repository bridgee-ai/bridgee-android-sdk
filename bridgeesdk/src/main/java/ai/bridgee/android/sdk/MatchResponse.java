package ai.bridgee.android.sdk;

import android.os.Bundle;

/**
 * Response object containing UTM attribution parameters from the Bridgee API.
 */
public class MatchResponse {
    private final String utmSource;
    private final String utmMedium;
    private final String utmCampaign;

    public MatchResponse(String utmSource, String utmMedium, String utmCampaign) {
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
    }

    /**
     * @return The UTM source parameter
     */
    public String getUtmSource() { 
        return utmSource; 
    }
    
    /**
     * @return The UTM medium parameter
     */
    public String getUtmMedium() { 
        return utmMedium; 
    }
    
    /**
     * @return The UTM campaign parameter
     */
    public String getUtmCampaign() { 
        return utmCampaign; 
    }

    /**
     * Converts the response to an Android Bundle for analytics integration.
     * @return Bundle containing UTM parameters
     */
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
    
    @Override
    public String toString() {
        return "MatchResponse{" +
                "utmSource='" + utmSource + '\'' +
                ", utmMedium='" + utmMedium + '\'' +
                ", utmCampaign='" + utmCampaign + '\'' +
                '}';
    }
}
