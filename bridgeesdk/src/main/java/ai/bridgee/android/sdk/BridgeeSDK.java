package ai.bridgee.android.sdk;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import ai.bridgee.android.sdk.internal.api.MatchApiClient;
import ai.bridgee.android.sdk.internal.api.InstallReferrerResolver;
import ai.bridgee.android.sdk.internal.model.MatchRequest;
import ai.bridgee.android.sdk.internal.model.MatchResponse;
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
     * This method will log a campaign details event into your analytics provider, but before that it will try to match
     * the given matchParameters with Bridgee-API. We want to inject in this event the UTM attribution parameters.
     * @param matchBundle Additional parameters to be matched - wich more parameters you provide, more accurate the match will be.
     */
    public void logCampaignDetailsEvent(MatchBundle matchBundle) {
        logCustomEvent("campaign_details", new Bundle(), matchBundle);
    }

    /**
     * This method will log an event into your analytics provider, but before that it will try to match
     * the given parameters with Bridgee-API. We want to inject in this event the UTM attribution parameters.
     * @param matchParams Additional parameters to be matched - wich more parameters you provide, more accurate the match will be.
     */
    public void logCustomEvent(String eventName, Bundle ep, MatchBundle mb) {
        try {
            Bundle eventParams = cloneBundle(ep);
            MatchBundle matchBundle = cloneMatchBundle(mb);

            if (eventName == null || eventName.trim().isEmpty()) {
                Log.w(TAG, "Event name cannot be null or empty");
                return;
            }

            matchBundle.withCustomParam("event_name", eventName);

            // First resolve install referrer, then make the match call
            resolveInstallReferrer(matchBundle, new Runnable() {
                @Override
                public void run() {
                    // Only call match after install referrer is resolved
                    match(matchBundle.toBundle(), new MatchCallback<MatchResponse>() {
                        @Override
                        public void onSuccess(MatchResponse response) {
                            Log.d(TAG, "Dry run: " + eventName + " >> " + dryRun + " >> " + response.toBundle());
                            if (!dryRun) {
                                eventParams.putAll(response.toBundle());
                                analyticsProvider.logEvent(eventName, eventParams);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            // we garantee that if something goes wrong, we will not throw
                            // an exception and eventually break the app experience
                            Log.e(TAG, "error to log event: " + error);
                        }
                    });
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, "error to log event: " + eventName + " >> " + e.getMessage());
        }
    }

    /****** PRIVATE METHODS *******/

    private void resolveInstallReferrer(MatchBundle matchBundle, Runnable onComplete) {
        instalReferrerResolver.resolve(new InstallReferrerResolver.ResolveCallback() {
            @Override
            public void onResolve(String installReferrer) {
                if (installReferrer != null && !installReferrer.trim().isEmpty()) {
                    matchBundle.withCustomParam("install_referrer", installReferrer);
                    Log.d(TAG, "install referrer resolved and added to match bundle: " + installReferrer);
                } 
                else {
                    matchBundle.withCustomParam("install_referrer", "error:not_found");
                    Log.d(TAG, "No install referrer found");
                }
                
                // Execute the callback after install referrer is resolved
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
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

    private <T extends MatchResponse> void match(Bundle searchBundle, final MatchCallback<T> callback) {
        if (searchBundle == null) {
            throw new IllegalArgumentException("SearchParams cannot be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("MatchCallback cannot be null");
        }

        // Create a new MatchApiClient instance for each call to avoid singleton issues
        MatchApiClient matchApiClient = new MatchApiClient(context, tenantId, tenantKey);

        MatchApiClient.MatchCallback<JSONObject> apiCallback = new MatchApiClient.MatchCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    MatchResponse matchResponse = new MatchResponse(
                        response.optString("utm_source", response.getString("utm_source")),
                        response.optString("utm_medium", response.getString("utm_medium")),
                        response.optString("utm_campaign", response.getString("utm_campaign"))
                    );
                    
                    callback.onSuccess((T) matchResponse);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response: " + e.getMessage(), e);
                    callback.onError("Error processing response from server");
                } finally {
                    // Cleanup the client after use
                    matchApiClient.shutdown();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error in API call: " + error);
                callback.onError(error);
                // Cleanup the client after error
                matchApiClient.shutdown();
            }
        };
        
        matchApiClient.match(searchBundle, apiCallback);
    }

    public interface MatchCallback<T extends MatchResponse> {
        void onSuccess(T response);
        void onError(String error);
    }
}
