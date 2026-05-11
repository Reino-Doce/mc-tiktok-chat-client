package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RenderedCommentTracker {
    private static final int FINGERPRINT_TTL_SECONDS = 6;
    private static final long FINGERPRINT_TTL_MILLIS = Duration.ofSeconds(FINGERPRINT_TTL_SECONDS).toMillis();
    private static final String SEPARATOR = "|";

    private final Map<String, Long> fingerprints = new ConcurrentHashMap<>();

    boolean wasRecentlyRendered(String username, String message) {
        long now = System.currentTimeMillis();
        cleanup(now);
        return fingerprints.containsKey(fingerprint(username, message));
    }

    void remember(String username, String message) {
        String sanitized = MessageSanitizer.sanitize(message);
        if (sanitized.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        cleanup(now);
        fingerprints.put(fingerprint(username, sanitized), now);
    }

    void clear() {
        fingerprints.clear();
    }

    private void cleanup(long now) {
        fingerprints.entrySet().removeIf(entry -> now - entry.getValue() > FINGERPRINT_TTL_MILLIS);
    }

    private static String fingerprint(String username, String message) {
        return TikTokUserNames.sanitizeUserName(username) + SEPARATOR + MessageSanitizer.sanitize(message);
    }
}
