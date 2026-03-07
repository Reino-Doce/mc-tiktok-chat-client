package br.com.reinodoce.mctiktok.util;

public final class UsernameValidator {
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9._]{2,30}$";

    private UsernameValidator() {
    }

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

    public static boolean isValid(String normalizedUsername) {
        return normalizedUsername != null && normalizedUsername.matches(USERNAME_PATTERN);
    }
}
