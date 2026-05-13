package br.com.reinodoce.mctiktok.state;

import java.time.Instant;

/**
 * Thread-safe mutable state for the current TikTok LIVE connection lifecycle.
 */
@SuppressWarnings("PMD.DataClass")
public class LiveSessionState {
    private ConnectionLifecycleState state = ConnectionLifecycleState.DISCONNECTED;
    private String username = "";
    private String lastError = "";
    private Instant reconnectAt;
    private int reconnectAttempts;

    /**
     * Returns an immutable snapshot of the current connection state.
     *
     * @return current state snapshot
     */
    public synchronized Snapshot snapshot() {
        return new Snapshot(state, username, lastError, reconnectAt, reconnectAttempts);
    }

    /**
     * Updates the lifecycle state.
     *
     * @param state new lifecycle state
     */
    public synchronized void setState(ConnectionLifecycleState state) {
        this.state = state;
    }

    /**
     * Updates the active or target username.
     *
     * @param username username to store
     */
    public synchronized void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    /**
     * Updates the last connection error text.
     *
     * @param lastError error text to store
     */
    public synchronized void setLastError(String lastError) {
        this.lastError = lastError == null ? "" : lastError;
    }

    /**
     * Updates the scheduled reconnect instant.
     *
     * @param reconnectAt reconnect instant, or {@code null} when no reconnect is scheduled
     */
    public synchronized void setReconnectAt(Instant reconnectAt) {
        this.reconnectAt = reconnectAt;
    }

    /**
     * Sets the reconnect-attempt counter.
     *
     * @param reconnectAttempts reconnect attempts, clamped to zero or greater
     */
    public synchronized void setReconnectAttempts(int reconnectAttempts) {
        this.reconnectAttempts = Math.max(0, reconnectAttempts);
    }

    /**
     * Increments and returns the reconnect-attempt counter.
     *
     * @return incremented reconnect-attempt count
     */
    public synchronized int incrementReconnectAttempts() {
        reconnectAttempts = Math.max(0, reconnectAttempts) + 1;
        return reconnectAttempts;
    }

    /**
     * Immutable connection state snapshot.
     *
     * @param state lifecycle state
     * @param username active or target username
     * @param lastError last connection error text
     * @param reconnectAt scheduled reconnect instant, or {@code null}
     * @param reconnectAttempts reconnect-attempt count
     */
    public record Snapshot(
            ConnectionLifecycleState state,
            String username,
            String lastError,
            Instant reconnectAt,
            int reconnectAttempts
    ) {
    }
}
