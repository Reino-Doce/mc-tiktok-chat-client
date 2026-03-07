package br.com.reinodoce.mctiktok.state;

import java.time.Instant;

public class LiveSessionState {
    private ConnectionLifecycleState state = ConnectionLifecycleState.DISCONNECTED;
    private String username = "";
    private String lastError = "";
    private Instant reconnectAt;
    private int reconnectAttempts;

    public synchronized Snapshot snapshot() {
        return new Snapshot(state, username, lastError, reconnectAt, reconnectAttempts);
    }

    public synchronized void setState(ConnectionLifecycleState state) {
        this.state = state;
    }

    public synchronized void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    public synchronized void setLastError(String lastError) {
        this.lastError = lastError == null ? "" : lastError;
    }

    public synchronized void setReconnectAt(Instant reconnectAt) {
        this.reconnectAt = reconnectAt;
    }

    public synchronized void setReconnectAttempts(int reconnectAttempts) {
        this.reconnectAttempts = Math.max(0, reconnectAttempts);
    }

    public synchronized int incrementReconnectAttempts() {
        reconnectAttempts = Math.max(0, reconnectAttempts) + 1;
        return reconnectAttempts;
    }

    public record Snapshot(
            ConnectionLifecycleState state,
            String username,
            String lastError,
            Instant reconnectAt,
            int reconnectAttempts
    ) {
    }
}
