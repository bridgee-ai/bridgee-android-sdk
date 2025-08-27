package ai.bridgee.android.sdk.internal.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

public class TenantTokenEncoder {
    
    public static String encodeToken(String id, String key) {
        if (id == null || id.isEmpty() || key == null || key.isEmpty()) {
            throw new IllegalArgumentException("ID and key must not be null or empty");
        }

        String combined = id + ";" + key;
        
        String base64 = Base64.encodeToString(
            combined.getBytes(StandardCharsets.UTF_8), 
            Base64.NO_WRAP
        );
        
        return base64;
    }
}
