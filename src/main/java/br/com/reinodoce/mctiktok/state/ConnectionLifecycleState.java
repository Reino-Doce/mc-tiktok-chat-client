package br.com.reinodoce.mctiktok.state;

public enum ConnectionLifecycleState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECT_SCHEDULED,
    ERROR
}
