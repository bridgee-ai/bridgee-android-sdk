package ai.bridgee.android.sdk.internal.util;

public interface ResponseCallback<T> {
    void ok(T response);
    void error(Exception e);
}
