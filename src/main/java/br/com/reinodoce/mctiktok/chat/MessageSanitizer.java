package br.com.reinodoce.mctiktok.chat;

public final class MessageSanitizer {
    private static final int MAX_TEXT_LENGTH = 256;

    private MessageSanitizer() {
    }

    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current == '\u0000') {
                continue;
            }
            if (Character.isISOControl(current) && !Character.isWhitespace(current)) {
                continue;
            }
            sanitized.append(current);
            if (sanitized.length() >= MAX_TEXT_LENGTH) {
                break;
            }
        }

        return sanitized.toString().replaceAll("\\s+", " ").trim();
    }
}
