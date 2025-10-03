package ai.bridgee.android.sdk.internal.api;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ai.bridgee.android.sdk.internal.model.MatchRequest;
import ai.bridgee.android.sdk.internal.util.TenantTokenEncoder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MatchApiClient {
    private static final String TAG = "BridgeeSDK";
    private static final String BASE_URL = "https://api.bridgee.ai/";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final int CONNECTION_TIMEOUT_MS = 500; // 0.5 segundos
    private static final int READ_TIMEOUT_MS = 1500; // 1.5 segundos
    
    private final Gson gson;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Context context;
    private final String tenantId;
    private final String tenantKey;

    public MatchApiClient(Context context, String tenantId, String tenantKey) {
        this.context = context.getApplicationContext();
        this.tenantId = tenantId;
        this.tenantKey = tenantKey;
        this.gson = new GsonBuilder().create();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    public void match(
        android.os.Bundle bundle,
        final MatchCallback<JSONObject> callback
    ) {
        if (!isNetworkAvailable()) {
            notifyError(callback, "Sem conexão com a internet");
            return;
        }

        executorService.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                MatchRequest matchRequest = MatchRequest.fromBundle(bundle);
                String json = gson.toJson(matchRequest);
                
                URL url = new URL(BASE_URL + "match");
                urlConnection = (HttpURLConnection) url.openConnection();
                
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", CONTENT_TYPE);
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                urlConnection.setReadTimeout(READ_TIMEOUT_MS);
                urlConnection.setDoOutput(true);
                
                if (tenantId != null && tenantKey != null) {
                    try {
                        String token = TenantTokenEncoder.encodeToken(tenantId, tenantKey);
                        urlConnection.setRequestProperty("x-tenant-token", token);
                        Log.d(TAG, "Added x-tenant header to request");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to generate x-tenant token", e);
                    }
                }
                
                try (DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream())) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    wr.write(input, 0, input.length);
                }
                
                int responseCode = urlConnection.getResponseCode();
                
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(
                            (responseCode == HttpURLConnection.HTTP_OK) ? 
                            urlConnection.getInputStream() : 
                            urlConnection.getErrorStream(), 
                            StandardCharsets.UTF_8))) {
                    
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    notifySuccess(callback, jsonResponse);
                } else {
                    String errorMsg = "Error from server: " + responseCode + " - " + response.toString();
                    notifyError(callback, errorMsg);
                }
                
            } catch (Exception e) {
                notifyError(callback, e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    private void notifySuccess(MatchCallback<JSONObject> callback, JSONObject response) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(response));
        }
    }

    private void notifyError(MatchCallback<?> callback, String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(error));
        }
    }

    /**
     * Cleanup method to shutdown the executor service
     * Should be called when this client instance is no longer needed
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public interface MatchCallback<T> {
        void onSuccess(T response);
        void onError(String error);
    }
}
