package br.com.reinodoce.mctiktok.pinned;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

/**
 * Client-side boundary for rendering TikTok pinned messages without using chat packets.
 */
public interface PinnedMessageSink {
    /**
     * Shows a pinned message through a local-only client overlay.
     *
     * @param config runtime configuration snapshot
     * @param message sanitized pinned message
     */
    void showPinnedMessage(ReinodoceConfig config, PinnedLiveMessage message);

    /**
     * Clears retained pinned overlay messages.
     */
    default void clearPinnedMessages() {
    }

    /**
     * Creates a sink that intentionally drops pinned messages.
     *
     * @return no-op pinned message sink
     */
    static PinnedMessageSink noop() {
        return new PinnedMessageSink() {
            @Override
            public void showPinnedMessage(ReinodoceConfig config, PinnedLiveMessage message) {
            }
        };
    }
}
