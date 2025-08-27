package ai.bridgee.android.sdk.internal.model;

import com.google.gson.annotations.SerializedName;

public class MetadataItem {
    @SerializedName("key")
    private String key;
    
    @SerializedName("value")
    private String value;

    public MetadataItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // Getters e Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
