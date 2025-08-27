package ai.bridgee.android.sdk.internal.model;

import android.os.Bundle;
import com.google.gson.annotations.SerializedName;

import ai.bridgee.android.sdk.internal.model.MetadataItem;
import java.util.ArrayList;
import java.util.List;

public class MatchRequest {

    @SerializedName("metadata")
    private List<MetadataItem> metadata;

    public MatchRequest(List<MetadataItem> metadata) {
        this.metadata = metadata != null ? metadata : new ArrayList<>();
    }

    public List<MetadataItem> getMetadata() {
        return metadata;
    }
    
    public static MatchRequest fromBundle(Bundle bundle) {
        if (bundle == null) {
            return new MatchRequest(new ArrayList<>());
        }
        
        List<MetadataItem> metadata = new ArrayList<>();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value != null) {
                metadata.add(new MetadataItem(key, value.toString()));
            }
        }
        return new MatchRequest(metadata);
    }

    public void setMetadata(List<MetadataItem> metadata) {
        this.metadata = metadata != null ? metadata : new ArrayList<>();
    }
}
