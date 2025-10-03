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
public class InstallReferrerResolver {
    
    private static final String TAG = "InstallReferrerResolver";
    
    private final Context context;
    private InstallReferrerClient referrerClient;
    
    public InstallReferrerResolver(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Resolves the bfpid parameter from the install referrer URL
     * @param callback Callback to receive the bfpid value or null if not found
     */
    public void resolve(ResolveCallback callback) {
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
                            if (referrerUrl == null || referrerUrl.trim().isEmpty())
                                callback.onResolve("error:empty");
                            else
                                callback.onResolve("success:" + referrerUrl);
                        } 
                        catch (RemoteException e) {
                            Log.e(TAG, "RemoteException getting install referrer", e);
                            callback.onResolve("error:" + e.getMessage());
                        } finally {
                            referrerClient.endConnection();
                        }
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        Log.w(TAG, "Install Referrer API not supported");
                        callback.onResolve("error:not_supported");
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        Log.w(TAG, "Install Referrer service unavailable");
                        callback.onResolve("error:service_unavailable");
                        break;
                    default:
                        Log.w(TAG, "Install Referrer setup failed with code: " + responseCode);
                        callback.onResolve("error:setup_failed");
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                Log.d(TAG, "Install Referrer service disconnected");
                callback.onResolve("error:service_disconnected");
            }
        });
    }
    
    /**
     * Callback interface for bfpid resolution
     */
    public interface ResolveCallback {
        void onResolve(String result);
    }
}
