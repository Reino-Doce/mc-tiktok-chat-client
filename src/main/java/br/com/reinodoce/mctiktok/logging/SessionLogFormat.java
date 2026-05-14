package br.com.reinodoce.mctiktok.logging;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Supported local session log formats.
 */
public enum SessionLogFormat {
    /** JSON Lines, one event object per line. */
    JSONL("jsonl", ".jsonl"),
    /** Plain text key-value lines. */
    TEXT("text", ".txt");

    private final String idValue;
    private final String extensionValue;

    SessionLogFormat(String id, String extension) {
        this.idValue = id;
        this.extensionValue = extension;
    }

    /**
     * Returns the persisted format identifier.
     *
     * @return format id
     */
    public String id() {
        return idValue;
    }

    /**
     * Returns the file extension for this format.
     *
     * @return file extension with leading dot
     */
    public String extension() {
        return extensionValue;
    }

    /**
     * Returns all format identifiers for command suggestions.
     *
     * @return supported ids
     */
    public static List<String> ids() {
        return Arrays.stream(values())
                .map(SessionLogFormat::id)
                .toList();
    }

    /**
     * Parses a persisted format id, defaulting safely to JSONL.
     *
     * @param value persisted value
     * @return parsed format
     */
    public static SessionLogFormat fromString(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (SessionLogFormat format : values()) {
            if (format.idValue.equals(normalized)) {
                return format;
            }
        }
        return JSONL;
    }
}
