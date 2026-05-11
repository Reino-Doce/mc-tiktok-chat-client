package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.i18n.Translations;
import io.github.jwdeveloper.tiktok.data.models.users.User;

final class TikTokUserNames {
    static final String USER_UNKNOWN_KEY = "reinodoce.chat.user_unknown";
    private static final String NONE_LITERAL = "None";

    private TikTokUserNames() {
    }

    static String sanitizeUserName(String raw) {
        String sanitized = MessageSanitizer.sanitize(raw);
        return sanitized.isBlank() ? Translations.tr(USER_UNKNOWN_KEY) : sanitized;
    }

    static String resolveUserName(User user) {
        if (user == null) {
            return Translations.tr(USER_UNKNOWN_KEY);
        }
        if (user.getProfileName() != null && !user.getProfileName().isBlank()) {
            return user.getProfileName();
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return Translations.tr(USER_UNKNOWN_KEY);
    }

    static String chooseRawUserName(String profileName, String username) {
        if (profileName != null && !profileName.isBlank()) {
            return profileName;
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        return Translations.tr(USER_UNKNOWN_KEY);
    }

    static boolean isKnownRawUser(io.github.jwdeveloper.tiktok.messages.data.User rawUser) {
        if (rawUser == null) {
            return false;
        }
        return rawUser.getId() > 0
                || !MessageSanitizer.sanitize(rawUser.getNickname()).isBlank()
                || !MessageSanitizer.sanitize(rawUser.getUsername()).isBlank();
    }

    static String normalizeErrorReason(String rawReason, String fallback) {
        String reason = MessageSanitizer.sanitize(rawReason);
        if (reason.isBlank() || NONE_LITERAL.equalsIgnoreCase(reason)) {
            return fallback;
        }
        return reason;
    }
}
