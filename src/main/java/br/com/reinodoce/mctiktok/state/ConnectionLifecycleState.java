package br.com.reinodoce.mctiktok.state;

/**
 * User-visible TikTok LIVE connection lifecycle states.
 */
public enum ConnectionLifecycleState {
    /** No active TikTok LIVE connection exists. */
    DISCONNECTED,
    /** A connection attempt is in progress. */
    CONNECTING,
    /** The TikTok LIVE websocket is connected. */
    CONNECTED,
    /** A reconnect attempt has been scheduled after an error. */
    RECONNECT_SCHEDULED,
    /** The connection stopped because of an unrecovered error. */
    ERROR
}
