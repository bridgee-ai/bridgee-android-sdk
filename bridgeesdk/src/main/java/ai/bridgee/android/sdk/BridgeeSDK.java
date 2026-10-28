package ai.bridgee.android.sdk;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import ai.bridgee.android.sdk.internal.api.MatchApiClient;
import ai.bridgee.android.sdk.internal.api.InstallReferrerResolver;
import ai.bridgee.android.sdk.internal.model.MatchRequest;
import ai.bridgee.android.sdk.internal.util.TenantTokenEncoder;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * BridgeeSDK is a SDK that publish events into an given analytics provider (e.g. Firebase Analytics).
 * The main purpose is to inject UTM atribution parameters into the events, according to a proprietary
 * match logic thats searchs in our captured data the best atribution event given the search parameters
 * you provided. Wich more search parameters you provide, more accurate the match will be.
 */
public class BridgeeSDK {

    private static final String TAG = "BRIDGEE-SDK";
    private final AnalyticsProvider analyticsProvider;
    private final Context context;
    private final InstallReferrerResolver instalReferrerResolver;
    private final String tenantId;
    private final String tenantKey;
    private Boolean dryRun = true;

    private static BridgeeSDK instance;

    private static final String FIRST_OPEN_EVENT_NAME = "first_open";
    private static final String CAMPAIGN_DETAILS_EVENT_NAME = "campaign_details";

    /****** PUBLIC METHODS *******/

    /**
     * This is the constructor of BridgeeSDK. We recommend to use this as a singleton pattern.
     *
     * @param context The application context.
     * @param provider The analytics provider implementation provided by the client.
     * @param tenantId The tenant ID for authentication.
     * @param tenantKey The tenant key for authentication.
     * @param dryRun Boolean to enable dry run mode.
     */
    public static synchronized BridgeeSDK getInstance(Context context, AnalyticsProvider provider, String tenantId, String tenantKey, Boolean dryRun) {
        if (instance == null) {
            instance = new BridgeeSDK(context, provider, tenantId, tenantKey, dryRun);
        }
        return instance;
    }

    public void firstOpen(MatchBundle mb) {
        this.firstOpen(mb, null);
    }

    /**
     * Register some user attributes and events to firebase so we vinculate the installment to the right channel.
     * 
     * @param mb user data that you can send to help us match the user with the right attribution event.
     * the more data you provide, the more accurate the match will be.
     */
    public void firstOpen(MatchBundle mb, ResponseCallback<MatchResponse> callback) {
        MatchBundle matchBundle = cloneMatchBundle(mb);
        matchBundle.withCustomParam("event_name", FIRST_OPEN_EVENT_NAME);

        resolveAttribution(matchBundle, new ResponseCallback<MatchResponse>() {
            @Override
            public void ok(MatchResponse matchResponse) {
                Log.d(TAG, "Attribution resolved");

                deliverAttribution(matchResponse);

                if (callback != null)
                    callback.ok(matchResponse);
            }

            @Override
            public void error(Exception e) {
                Log.e(TAG, "Failed to resolve attribution");
                if (callback != null)
                    callback.error(e);
            }
        });
        
    }

    /** Explicit policy entry: denied consent and native Google never call the match API. */
    public void firstOpenWithConsent(MatchBundle bundle, boolean consentGranted,
            boolean preserveNativeGoogleAttribution, ResponseCallback<MatchResponse> callback) {
        if (!consentGranted) {
            if (callback != null) callback.error(new IllegalStateException("attribution_consent_required"));
            return;
        }
        if (preserveNativeGoogleAttribution) {
            if (callback != null) callback.ok(new MatchResponse("", "", ""));
            return;
        }
        firstOpen(bundle, callback);
    }

    // Visibilidade de pacote permite testar a entrega sem chamar a API real.
    void deliverAttribution(MatchResponse response) {
        setUserProperty("install_source", response.getUtmSource());
        setUserProperty("install_medium", response.getUtmMedium());
        setUserProperty("install_campaign", response.getUtmCampaign());
        // Firebase é responsável por first_open; Bridgee entrega apenas a campanha.
        logEvent(tenantId + "_" + CAMPAIGN_DETAILS_EVENT_NAME, response.toBundle());
        logEvent(CAMPAIGN_DETAILS_EVENT_NAME, response.toBundle());
    }

    /****** PRIVATE METHODS *******/

    private void setUserProperty(String name, String value) {
        Log.d(TAG, "Setting attribution user property");
        if (!dryRun) {
            try {
                name = name.replace("-", "_");
                analyticsProvider.setUserProperty(name, value);
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to set attribution user property");
            }
        }
    }
    
    private void logEvent(String name, Bundle params) {
        Log.d(TAG, "Delivering attribution event");
        if (!dryRun) {
            try {
                name = name.replace("-", "_");
                analyticsProvider.logEvent(name, params);
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to deliver attribution event");
            }        
        }
    }

    private Bundle cloneBundle(Bundle bundle) {
        Bundle clone = new Bundle();
        clone.putAll(bundle);
        return clone;
    }

    private MatchBundle cloneMatchBundle(MatchBundle matchBundle) {
        Bundle clone = cloneBundle(matchBundle.toBundle());
        return new MatchBundle(clone);
    }

    BridgeeSDK(Context context, AnalyticsProvider provider, String tenantId, String tenantKey, Boolean dryRun) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("AnalyticsProvider cannot be null");
        }
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (tenantKey == null || tenantKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant key cannot be null or empty");
        }
        
        this.context = context.getApplicationContext();
        this.analyticsProvider = provider;
        this.tenantId = tenantId;
        this.tenantKey = tenantKey;
        this.instalReferrerResolver = new InstallReferrerResolver(this.context);
        this.dryRun = dryRun;
    }

    private void resolveAttribution(MatchBundle matchBundle, ResponseCallback<MatchResponse> callback) {
        resolveInstallReferrer(new ResponseCallback<String>() {
            @Override
            public void ok(String installReferrer) {
                
                matchBundle.withCustomParam("install_referrer", installReferrer);

                resolveMatch(matchBundle, new ResponseCallback<MatchResponse>() {
                    @Override
                    public void ok(MatchResponse matchResponse) {
                        Log.d(TAG, "Match resolved");
                        callback.ok(matchResponse);
                    }

                    @Override
                    public void error(Exception e) {
                        Log.e(TAG, "Failed to resolve match");
                        callback.error(e);
                    }
                });
            }

            @Override
            public void error(Exception e) {
                Log.e(TAG, "Failed to resolve install referrer");
                callback.error(e);
            }
        });
    }

    private void resolveInstallReferrer(ResponseCallback<String> callback) {
        instalReferrerResolver.resolve(new ResponseCallback<String>() {
            @Override
            public void ok(String installReferrer) {              
                Log.d(TAG, "Install referrer resolved");
                callback.ok(installReferrer);
            }

            @Override
            public void error(Exception e) {
                Log.e(TAG, "Failed to resolve install referrer");
                callback.error(e);
            }
        });
    }

    private <T extends MatchResponse> void resolveMatch(MatchBundle matchBundle, ResponseCallback<MatchResponse> callback) {
        // Create a new MatchApiClient instance for each call to avoid singleton issues
        MatchApiClient matchApiClient = new MatchApiClient(context, tenantId, tenantKey);

        matchApiClient.match(matchBundle.toBundle(), new ResponseCallback<JSONObject>() {
            @Override
            public void ok(JSONObject response) {
                try {                    
                    MatchResponse matchResponse = new MatchResponse(
                        response.getString("utm_source"), 
                        response.getString("utm_medium"), 
                        response.getString("utm_campaign")
                    );

                    Log.d(TAG, "Match API call successful");
                    callback.ok(matchResponse);
                } 
                catch (Exception e) {
                    Log.e(TAG, "Error processing Match API response");
                    callback.error(e);
                } 
                finally {
                    // Cleanup the client after use
                    matchApiClient.shutdown();
                }
            }

            @Override
            public void error(Exception e) {
                // Cleanup the client after error
                matchApiClient.shutdown();
                
                Log.e(TAG, "Error in Match API call");
                callback.error(e);
            }
        });
    }
}
