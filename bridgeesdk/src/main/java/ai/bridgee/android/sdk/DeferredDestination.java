package ai.bridgee.android.sdk;

/** Destination to route inside the host app; resolving never performs navigation or attribution. */
public final class DeferredDestination {
    public final String blinklink;
    public final String appLink;
    public DeferredDestination(String blinklink, String appLink) {
        this.blinklink = blinklink; this.appLink = appLink;
    }
}
