package ai.bridgee.android.sdk;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import ai.bridgee.android.sdk.internal.api.MatchApiClient;
import ai.bridgee.android.sdk.internal.api.InstallRefererResolver;
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
    private final MatchApiClient matchApiClient;
    private final Context context;
    private final InstallRefererResolver instalRefererResolver;
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
     * This method will log an event into your analytics provider, but before that it will try to match
     * the given parameters with Bridgee-API. We want to inject in this event the UTM attribution parameters.
     * @param eventName The name of the event to be logged.
     * @param matchParams Additional parameters to be matched - wich more parameters you provide, more accurate the match will be.
     */
    public void logEvent(String eventName, Bundle eventParams, MatchBundle matchBundle) {
        try {
            if (eventName == null || eventName.trim().isEmpty()) {
                Log.w(TAG, "Event name cannot be null or empty");
                return;
            }

            resolveBfpid(matchBundle);

            match(matchBundle.toBundle(), new MatchCallback<MatchResponse>() {
                @Override
                public void onSuccess(MatchResponse response) {
                    Log.d(TAG, "Dry run: " + eventName + " >> " + response.toBundle());
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
        } catch (Exception e) {
            // we garantee that if something goes wrong, we will not throw
            // an exception and eventually break the app experience
            Log.e(TAG, "error to log event: " + e.getMessage(), e);
        }
    }

    /****** PRIVATE METHODS *******/

    private void resolveBfpid(MatchBundle matchBundle) {
        instalRefererResolver.resolveBfpid(new InstallRefererResolver.BfpidCallback() {
            @Override
            public void onBfpidResolved(String bfpid) {
                if (bfpid != null && !bfpid.trim().isEmpty()) {
                    matchBundle.withCustomParam("bfpid", bfpid);
                    Log.d(TAG, "bfpid resolved and added to match bundle: " + bfpid);
                } else {
                    Log.d(TAG, "No bfpid found in install referrer");
                }
            }
        });
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
        this.matchApiClient = MatchApiClient.getInstance(this.context, tenantId, tenantKey);
        this.instalRefererResolver = new InstallRefererResolver(this.context);
        this.dryRun = dryRun;
    }

    private <T extends MatchResponse> void match(Bundle searchBundle, final MatchCallback<T> callback) {
        if (searchBundle == null) {
            throw new IllegalArgumentException("SearchParams cannot be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("MatchCallback cannot be null");
        }

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
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error in API call: " + error);
                callback.onError(error);
            }
        };
        
        matchApiClient.match(searchBundle, apiCallback);
    }

    public interface MatchCallback<T extends MatchResponse> {
        void onSuccess(T response);
        void onError(String error);
    }
}
