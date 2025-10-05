package ai.bridgee.android.sdk;

import android.os.Bundle;

/**
 * This interface is used to provide a contract for the analytics provider.
 * The client of the SDK must provide an implementation of this interface
 * to allow the logging of events. For example, if you are using Firebase Analytics,
 * you can provide an implementation of this interface that will log events to Firebase Analytics.
 */
public interface AnalyticsProvider {

    /**
     * Logs an event.
     *
     * @param name   The name of the event to be logged.
     * @param params A Bundle containing the parameters of the event.
     */
    void logEvent(String name, Bundle params);

    /**
     * Sets a user property.
     *
     * @param name  The name of the user property to be set.
     * @param value The value of the user property to be set.
     */
    void setUserProperty(String name, String value);

}
