package br.com.reinodoce.mctiktok.pinned;

import java.time.Duration;

/**
 * Sanitized TikTok pinned-message payload for client-only overlay rendering.
 *
 * @param pinId TikTok pin identifier, or zero when unavailable
 * @param username display username
 * @param message pinned message text
 * @param duration visible duration
 */
public record PinnedLiveMessage(long pinId, String username, String message, Duration duration) {
    /** Default local lifetime when TikTok does not include a display duration. */
    public static final Duration DEFAULT_DURATION = Duration.ofSeconds(15);

    /**
     * Normalizes null payload fields.
     *
     * @param pinId TikTok pin identifier, or zero when unavailable
     * @param username display username
     * @param message pinned message text
     * @param duration visible duration
     */
    public PinnedLiveMessage {
        username = username == null ? "" : username;
        message = message == null ? "" : message;
        duration = duration == null || duration.isZero() || duration.isNegative() ? DEFAULT_DURATION : duration;
    }
}
