package br.com.reinodoce.mctiktok.chat;

/**
 * Normalizes text received from TikTok before it is rendered into Minecraft chat.
 */
public final class MessageSanitizer {
    private static final int MAX_TEXT_LENGTH = 256;
    private static final char NULL_CHAR = '\u0000';

    private MessageSanitizer() {
    }

    /**
     * Removes null/control characters, collapses whitespace, and caps visible message length.
     *
     * @param raw source text
     * @return safe chat text
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current == NULL_CHAR) {
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
