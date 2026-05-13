package br.com.reinodoce.mctiktok.util;

/**
 * Normalizes and validates TikTok usernames accepted by the command surface.
 */
public final class UsernameValidator {
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9._]{2,30}$";

    private UsernameValidator() {
    }

    /**
     * Trims whitespace and strips leading at-signs from a username.
     *
     * @param raw raw username input
     * @return normalized username or blank
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw.trim();
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        return normalized.trim();
    }

    /**
     * Validates a normalized username against the supported TikTok username shape.
     *
     * @param normalizedUsername normalized username
     * @return true when the username is syntactically valid
     */
    public static boolean isValid(String normalizedUsername) {
        return normalizedUsername != null && normalizedUsername.matches(USERNAME_PATTERN);
    }
}
