package br.com.reinodoce.mctiktok.util;

import java.time.Duration;
import java.time.Instant;

public class NoticeThrottler {
    private final Duration cooldown;
    private String lastKey = "";
    private Instant lastAt = Instant.EPOCH;

    public NoticeThrottler(Duration cooldown) {
        this.cooldown = cooldown;
    }

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

    public synchronized void reset() {
        lastKey = "";
        lastAt = Instant.EPOCH;
    }
}
