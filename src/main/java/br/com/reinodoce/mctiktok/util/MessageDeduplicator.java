package br.com.reinodoce.mctiktok.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Time-window deduplicator for TikTok message ids and combo counts.
 */
public class MessageDeduplicator {
    private final Map<String, Long> seenKeys = new ConcurrentHashMap<>();
    private final long ttlMillis;

    /**
     * Creates a deduplicator with the supplied entry lifetime.
     *
     * @param ttl time-to-live for remembered message keys
     */
    public MessageDeduplicator(Duration ttl) {
        this.ttlMillis = ttl.toMillis();
    }

    /**
     * Checks and records whether a message/count pair has already been seen.
     *
     * @param messageId TikTok message id
     * @param count event count or combo count
     * @return true when the pair was already seen inside the TTL window
     */
    public boolean isDuplicate(long messageId, int count) {
        if (messageId <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        cleanup(now);
        String key = messageId + ":" + Math.max(1, count);
        return seenKeys.putIfAbsent(key, now) != null;
    }

    /**
     * Clears all remembered message keys.
     */
    public void clear() {
        seenKeys.clear();
    }

    private void cleanup(long now) {
        seenKeys.entrySet().removeIf(entry -> now - entry.getValue() > ttlMillis);
    }
}
