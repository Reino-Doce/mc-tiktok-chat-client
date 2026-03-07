package br.com.reinodoce.mctiktok.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDeduplicator {
    private final Map<String, Long> seenKeys = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public MessageDeduplicator(Duration ttl) {
        this.ttlMillis = ttl.toMillis();
    }

    public boolean isDuplicate(long messageId, int count) {
        if (messageId <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        cleanup(now);
        String key = messageId + ":" + Math.max(1, count);
        return seenKeys.putIfAbsent(key, now) != null;
    }

    public void clear() {
        seenKeys.clear();
    }

    private void cleanup(long now) {
        seenKeys.entrySet().removeIf(entry -> now - entry.getValue() > ttlMillis);
    }
}
