package ai.bridgee.android.sdk.internal.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import ai.bridgee.android.sdk.ResponseCallback;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded, exactly-once Play referrer read, retaining the legacy success/error contract. */
public class InstallReferrerResolver {
    private final Context context;
    public InstallReferrerResolver(Context context) { this.context = context.getApplicationContext(); }

    public void resolve(ResponseCallback<String> callback) {
        final InstallReferrerClient client;
        try { client = InstallReferrerClient.newBuilder(context).build(); }
        catch (Exception e) { callback.ok("error:unavailable"); return; }
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean settled = new AtomicBoolean();
        Runnable timeout = () -> finish(client, settled, callback, "error:timeout");
        handler.postDelayed(timeout, 3000);
        try {
            client.startConnection(new InstallReferrerStateListener() {
                @Override public void onInstallReferrerSetupFinished(int code) {
                    if (settled.get()) return;
                    String value = "error:unavailable";
                    try {
                        if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
                            String raw = client.getInstallReferrer().getInstallReferrer();
                            value = raw == null || raw.trim().isEmpty() ? "error:empty"
                                : raw.length() > 4096 ? "error:too_large" : "success:" + raw;
                        }
                    } catch (Exception ignored) { }
                    handler.removeCallbacks(timeout);
                    finish(client, settled, callback, value);
                }
                @Override public void onInstallReferrerServiceDisconnected() {
                    handler.removeCallbacks(timeout);
                    finish(client, settled, callback, "error:service_disconnected");
                }
            });
        } catch (Exception ignored) {
            handler.removeCallbacks(timeout);
            finish(client, settled, callback, "error:setup_failed");
        }
    }
    private static void finish(InstallReferrerClient client, AtomicBoolean settled,
            ResponseCallback<String> callback, String value) {
        if (!settled.compareAndSet(false, true)) return;
        try { client.endConnection(); } catch (Exception ignored) { }
        callback.ok(value);
    }
}
