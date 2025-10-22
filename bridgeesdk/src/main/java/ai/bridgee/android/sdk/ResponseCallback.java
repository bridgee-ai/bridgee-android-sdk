package ai.bridgee.android.sdk;

/**
 * Callback interface for asynchronous operations in the Bridgee SDK.
 * @param <T> The type of response expected
 */
public interface ResponseCallback<T> {
    /**
     * Called when the operation completes successfully.
     * @param response The response object
     */
    void ok(T response);
    
    /**
     * Called when the operation fails.
     * @param e The exception that occurred
     */
    void error(Exception e);
}
