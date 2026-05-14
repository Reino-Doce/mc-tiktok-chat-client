package br.com.reinodoce.mctiktok.config;

import java.util.Arrays;
import java.util.Locale;

/**
 * Local destinations for mirrored TikTok output.
 */
public enum OutputMode {
    /** Current chat HUD behavior. */
    CHAT("chat"),
    /** Latest-message actionbar output. */
    ACTIONBAR("actionbar"),
    /** Persistent bounded HUD output. */
    HUD("hud"),
    /** Suppress visible mirrored output while keeping the connection active. */
    OFF("off");

    private final String identifier;

    OutputMode(String id) {
        this.identifier = id;
    }

    /**
     * Returns the persisted identifier.
     *
     * @return mode identifier
     */
    public String id() {
        return identifier;
    }

    /**
     * Parses a mode identifier.
     *
     * @param value user or config value
     * @return parsed mode, or {@link #CHAT} when unknown
     */
    public static OutputMode fromString(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (OutputMode mode : values()) {
            if (mode.id().equals(normalized)) {
                return mode;
            }
        }
        return CHAT;
    }

    /**
     * Returns command suggestion identifiers.
     *
     * @return mode identifiers
     */
    public static Iterable<String> ids() {
        return Arrays.stream(values()).map(OutputMode::id).toList();
    }
}
