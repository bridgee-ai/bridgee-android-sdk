package ai.bridgee.android.sdk;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import ai.bridgee.android.sdk.internal.api.MatchApiClient;
import ai.bridgee.android.sdk.internal.api.InstallReferrerResolver;
import ai.bridgee.android.sdk.internal.model.MatchRequest;
import ai.bridgee.android.sdk.internal.model.MatchResponse;
import ai.bridgee.android.sdk.internal.util.TenantTokenEncoder;
import ai.bridgee.android.sdk.internal.util.ResponseCallback;

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

    /**
     * Register some user attributes and events to firebase so we vinculate the installment to the right channel.
     * 
     * @param mb user data that you can send to help us match the user with the right attribution event.
     * the more data you provide, the more accurate the match will be.
     */
    public void firstOpen(MatchBundle mb) {
        MatchBundle matchBundle = cloneMatchBundle(mb);
        matchBundle.withCustomParam("event_name", FIRST_OPEN_EVENT_NAME);

        resolveAttribution(matchBundle, new ResponseCallback<MatchResponse>() {
            @Override
            public void ok(MatchResponse matchResponse) {
                Log.d(TAG, "Attribution resolved: " + matchResponse);

                // user properties
                Log.d(TAG, "Setting user properties");
                setUserProperty("install_source", matchResponse.getUtmSource());
                setUserProperty("install_medium", matchResponse.getUtmMedium());
                setUserProperty("install_campaign", matchResponse.getUtmCampaign());            

                // custom events
                Log.d(TAG, "Logging custom events");
                logEvent(tenantId + "_" + FIRST_OPEN_EVENT_NAME, matchResponse.toBundle());
                logEvent(tenantId + "_" + CAMPAIGN_DETAILS_EVENT_NAME, matchResponse.toBundle());

                // reserved events
                Log.d(TAG, "Logging reserved events");
                logEvent(FIRST_OPEN_EVENT_NAME, matchResponse.toBundle());
                logEvent(CAMPAIGN_DETAILS_EVENT_NAME, matchResponse.toBundle());
            }

            @Override
            public void error(Exception e) {
                Log.e(TAG, "Failed to resolve attribution: " + e.getMessage());
            }
        });
        
    }

    /****** PRIVATE METHODS *******/

    private void setUserProperty(String name, String value) {
        Log.d(TAG, "Setting user property: " + name + " >> " + value);
        if (!dryRun) {
            try {
                name = name.replace("-", "_");
                analyticsProvider.setUserProperty(name, value);
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to set user property: " + name + " >> " + value, e);
            }
        }
    }
    
    private void logEvent(String name, Bundle params) {
        Log.d(TAG, "Logging event: " + name + " >> " + params);
        if (!dryRun) {
            try {
                name = name.replace("-", "_");
                analyticsProvider.logEvent(name, params);
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to log event: " + name + " >> " + params, e);
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

    private BridgeeSDK(Context context, AnalyticsProvider provider, String tenantId, String tenantKey, Boolean dryRun) {
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
                        Log.d(TAG, "Match resolved: " + matchResponse);
                        callback.ok(matchResponse);
                    }

                    @Override
                    public void error(Exception e) {
                        Log.e(TAG, "Failed to resolve match: " + e.getMessage());
                        callback.error(e);
                    }
                });
            }

            @Override
            public void error(Exception e) {
                Log.e(TAG, "Failed to resolve install referrer: " + e.getMessage());
                callback.error(e);
            }
        });
    }

    private void resolveInstallReferrer(ResponseCallback<String> callback) {
        instalReferrerResolver.resolve(new ResponseCallback<String>() {
            @Override
            public void ok(String installReferrer) {              
                Log.d(TAG, "Install referrer resolved: " + installReferrer);
                callback.ok(installReferrer);
            }

            @Override
            public void error(Exception e) {
                Log.e(TAG, "Failed to resolve install referrer: " + e.getMessage());
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

                    Log.d(TAG, "Match API call successful: " + matchResponse.toBundle());
                    callback.ok(matchResponse);
                } 
                catch (Exception e) {
                    Log.e(TAG, "Error processing Match API response: " + e.getMessage(), e);
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
                
                Log.e(TAG, "Error in Match API call: " + e.getMessage());
                callback.error(e);
            }
        });
    }
}
