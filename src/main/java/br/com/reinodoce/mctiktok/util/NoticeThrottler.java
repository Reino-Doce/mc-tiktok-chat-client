package br.com.reinodoce.mctiktok.util;

import java.time.Duration;
import java.time.Instant;

/**
 * Suppresses repeated notices with the same key until a cooldown has elapsed.
 */
public class NoticeThrottler {
    private final Duration cooldown;
    private String lastKey = "";
    private Instant lastAt = Instant.EPOCH;

    /**
     * Creates a throttler.
     *
     * @param cooldown minimum interval before the same key can be emitted again
     */
    public NoticeThrottler(Duration cooldown) {
        this.cooldown = cooldown;
    }

    /**
     * Checks whether a notice key should be emitted at the current instant.
     *
     * @param key notice identity
     * @return true when the notice should be emitted
     */
    public synchronized boolean shouldEmit(String key) {
        return shouldEmit(key, Instant.now());
    }

    synchronized boolean shouldEmit(String key, Instant now) {
        String safeKey = key == null ? "" : key;
        if (!safeKey.equals(lastKey) || Duration.between(lastAt, now).compareTo(cooldown) >= 0) {
            lastKey = safeKey;
            lastAt = now;
            return true;
        }
        return false;
    }

    /**
     * Resets throttling state so the next key can be emitted immediately.
     */
    public synchronized void reset() {
        lastKey = "";
        lastAt = Instant.EPOCH;
    }
}
