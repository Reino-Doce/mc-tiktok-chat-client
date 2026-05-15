package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.util.UsernameValidator;
import io.github.jwdeveloper.tiktok.data.models.users.User;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

final class UserCooldownTracker {
    private static final int MILLIS_PER_SECOND = 1_000;
    private static final long UNKNOWN_USER_ID = 0L;

    private final Map<String, Long> usernames = new ConcurrentHashMap<>();

    boolean isCoolingDown(User user, String username, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            usernames.clear();
            return false;
        }
        String key = key(user, username);
        if (key.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        long ttlMillis = Math.max((long) cooldownSeconds * MILLIS_PER_SECOND, MILLIS_PER_SECOND);
        usernames.entrySet().removeIf(entry -> now - entry.getValue() > ttlMillis);
        return usernames.containsKey(key);
    }

    void remember(User user, String username, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            usernames.clear();
            return;
        }
        String key = key(user, username);
        if (!key.isBlank()) {
            usernames.put(key, System.currentTimeMillis());
        }
    }

    void clear() {
        usernames.clear();
    }

    private static String key(User user, String username) {
        long userId = TikTokUserNames.resolveUserId(user);
        if (userId > UNKNOWN_USER_ID) {
            return "id:" + userId;
        }
        String handle = user == null ? "" : UsernameValidator.normalize(user.getName());
        if (!UsernameValidator.isValid(handle)) {
            handle = UsernameValidator.normalize(username);
        }
        return UsernameValidator.isValid(handle) ? handle.toLowerCase(Locale.ROOT) : "";
    }
}
