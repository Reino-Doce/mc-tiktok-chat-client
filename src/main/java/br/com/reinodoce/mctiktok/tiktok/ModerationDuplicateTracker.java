package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ModerationDuplicateTracker {
    private static final int MILLIS_PER_SECOND = 1_000;

    private final Map<String, Long> messages = new ConcurrentHashMap<>();

    boolean isDuplicate(String message, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return false;
        }
        String key = MessageSanitizer.sanitize(message).toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        long ttlMillis = Duration.ofSeconds(cooldownSeconds).toMillis();
        cleanup(now, ttlMillis);
        Long previous = messages.get(key);
        return previous != null && now - previous <= ttlMillis;
    }

    void remember(String message, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return;
        }
        String key = MessageSanitizer.sanitize(message).toLowerCase(Locale.ROOT);
        if (!key.isBlank()) {
            long now = System.currentTimeMillis();
            long ttlMillis = Duration.ofSeconds(cooldownSeconds).toMillis();
            cleanup(now, ttlMillis);
            messages.put(key, now);
        }
    }

    void clear() {
        messages.clear();
    }

    private void cleanup(long now, long ttlMillis) {
        long retentionMillis = Math.max(ttlMillis, MILLIS_PER_SECOND);
        messages.entrySet().removeIf(entry -> now - entry.getValue() > retentionMillis);
    }
}
