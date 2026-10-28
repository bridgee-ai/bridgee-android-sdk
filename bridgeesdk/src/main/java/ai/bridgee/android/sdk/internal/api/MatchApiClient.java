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
import ai.bridgee.android.sdk.ResponseCallback;

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
    private final Context context;
    private final String tenantId;
    private final String tenantKey;

    public MatchApiClient(Context context, String tenantId, String tenantKey) {
        this.context = context.getApplicationContext();
        this.tenantId = tenantId;
        this.tenantKey = tenantKey;
        this.gson = new GsonBuilder().create();
        this.executorService = Executors.newSingleThreadExecutor();
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

    public void match(android.os.Bundle bundle, ResponseCallback<JSONObject> callback) {
        if (!isNetworkAvailable()) {
            callback.error(new Exception("without internet connection"));
            return;
        }

        executorService.execute(() -> {
            HttpURLConnection urlConnection = null;
            try {
                MatchRequest matchRequest = MatchRequest.fromBundle(bundle);
                String json = gson.toJson(matchRequest);
                
                URL url = new URL(BASE_URL + "match");
                urlConnection = (HttpURLConnection) url.openConnection();
                
                configureConnection(urlConnection);
                urlConnection.setRequestProperty("Content-Type", CONTENT_TYPE);
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                urlConnection.setReadTimeout(READ_TIMEOUT_MS);
                urlConnection.setDoOutput(true);
                
                String token = TenantTokenEncoder.encodeToken(tenantId, tenantKey);
                urlConnection.setRequestProperty("x-tenant-token", token);
                
                try (DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream())) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    wr.write(input, 0, input.length);
                }
                
                callback.ok(readResponse(urlConnection));
                
            } 
            catch (Exception e) {
                callback.error(e);
            } 
            finally {
                if (urlConnection != null) 
                    urlConnection.disconnect();
            }
        });
    }

    // Never forward the tenant credential through an HTTP redirect.
    static void configureConnection(HttpURLConnection connection) throws IOException {
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
    }

    static JSONObject readResponse(HttpURLConnection connection) throws IOException, JSONException {
        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            // Error bodies can echo identifiers or credentials. Expose only the stable status code.
            throw new IOException("match_http_error:" + status);
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            char[] chunk = new char[2048];
            int count;
            while ((count = reader.read(chunk)) != -1) {
                if (response.length() + count > 16384) throw new IOException("match_response_too_large");
                response.append(chunk, 0, count);
            }
        }
        try {
            return new JSONObject(response.toString());
        } catch (JSONException error) {
            throw new JSONException("match_invalid_response");
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
}
