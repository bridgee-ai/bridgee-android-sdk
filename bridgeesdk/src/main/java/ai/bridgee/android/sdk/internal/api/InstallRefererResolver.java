package ai.bridgee.android.sdk.internal.api;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

/**
 * Resolver for extracting bfpid from Google Play Install Referrer API
 */
public class InstallRefererResolver {
    
    private static final String TAG = "InstallRefererResolver";
    private static final String BFPID_PARAM = "bfpid";
    
    private final Context context;
    private InstallReferrerClient referrerClient;
    
    public InstallRefererResolver(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Resolves the bfpid parameter from the install referrer URL
     * @param callback Callback to receive the bfpid value or null if not found
     */
    public void resolveBfpid(BfpidCallback callback) {
        if (referrerClient != null) {
            referrerClient.endConnection();
        }
        
        referrerClient = InstallReferrerClient.newBuilder(context).build();
        
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        try {
                            ReferrerDetails response = referrerClient.getInstallReferrer();
                            String referrerUrl = response.getInstallReferrer();
                            String bfpid = extractBfpidFromUrl(referrerUrl);
                            callback.onBfpidResolved(bfpid);
                        } catch (RemoteException e) {
                            Log.e(TAG, "RemoteException getting install referrer", e);
                            callback.onBfpidResolved(null);
                        } finally {
                            referrerClient.endConnection();
                        }
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        Log.w(TAG, "Install Referrer API not supported");
                        callback.onBfpidResolved(null);
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        Log.w(TAG, "Install Referrer service unavailable");
                        callback.onBfpidResolved(null);
                        break;
                    default:
                        Log.w(TAG, "Install Referrer setup failed with code: " + responseCode);
                        callback.onBfpidResolved(null);
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                Log.d(TAG, "Install Referrer service disconnected");
                callback.onBfpidResolved(null);
            }
        });
    }
    
    /**
     * Extracts bfpid parameter from the referrer URL
     * @param referrerUrl The install referrer URL
     * @return The bfpid value or null if not found
     */
    private String extractBfpidFromUrl(String referrerUrl) {
        if (referrerUrl == null || referrerUrl.trim().isEmpty()) {
            Log.d(TAG, "Referrer URL is null or empty");
            return null;
        }
        
        try {
            Uri uri = Uri.parse(referrerUrl);
            String bfpid = uri.getQueryParameter(BFPID_PARAM);
            
            if (bfpid != null && !bfpid.trim().isEmpty()) {
                Log.d(TAG, "Found bfpid: " + bfpid);
                return bfpid;
            } else {
                Log.d(TAG, "bfpid parameter not found in referrer URL: " + referrerUrl);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing referrer URL: " + referrerUrl, e);
            return null;
        }
    }
    
    /**
     * Callback interface for bfpid resolution
     */
    public interface BfpidCallback {
        void onBfpidResolved(String bfpid);
    }
}
