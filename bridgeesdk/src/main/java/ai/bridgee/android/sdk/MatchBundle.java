package ai.bridgee.android.sdk;

import android.os.Bundle;

/**
 * This class is used to build a bundle of parameters to be matched with Bridgee-API.
 * It is a helper class to build a bundle of parameters to be matched with Bridgee-API.
 * Wich more parameters you provide, more accurate the match will be.
 */
public class MatchBundle {
    
    private Bundle params;

    public MatchBundle() {
        params = new Bundle();
    }

    public MatchBundle(Bundle bundle) {
        params = bundle;
    }

    public MatchBundle withCustomParam(String key, String value) {
        params.putString(key, value);
        return this;
    }

    public MatchBundle withEmail(String email) {
        params.putString("email", email);
        return this;
    }

    public MatchBundle withPhone(String phone) {
        params.putString("phone", phone);
        return this;
    }

    public MatchBundle withName(String name) {
        params.putString("name", name);
        return this;
    }

    public MatchBundle withGclid(String gclid) {
        params.putString("gclid", gclid);
        return this;
    }

    public Bundle toBundle() {
        return params;
    }

}
