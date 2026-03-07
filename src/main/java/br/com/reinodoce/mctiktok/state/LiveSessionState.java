package br.com.reinodoce.mctiktok.state;

import java.time.Instant;

public class LiveSessionState {
    private ConnectionLifecycleState state = ConnectionLifecycleState.DISCONNECTED;
    private String username = "";
    private String lastError = "";
    private Instant reconnectAt;

    public synchronized Snapshot snapshot() {
        return new Snapshot(state, username, lastError, reconnectAt);
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

    public record Snapshot(
            ConnectionLifecycleState state,
            String username,
            String lastError,
            Instant reconnectAt
    ) {
    }
}
