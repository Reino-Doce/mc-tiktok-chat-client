package br.com.reinodoce.mctiktok.alert;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Toast media selection mode for alert events.
 */
public enum AlertToastMediaMode {
    /** Render text only, using the platform default toast icon. */
    NONE("none"),
    /** Prefer the user's profile image. */
    PROFILE("profile"),
    /** Prefer the configured custom image. */
    CUSTOM("custom"),
    /** Prefer profile image, then custom image. */
    PROFILE_CUSTOM("profile-custom"),
    /** Prefer the gift image for gift alerts. */
    GIFT("gift"),
    /** Keep media references available for inline-capable renderers. */
    INLINE("inline");

    /** Default persisted media mode. */
    public static final AlertToastMediaMode DEFAULT = NONE;

    private final String identifier;

    AlertToastMediaMode(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the persisted command/config identifier.
     *
     * @return media mode identifier
     */
    public String id() {
        return identifier;
    }

    /**
     * Parses a media mode without applying a fallback.
     *
     * @param value command/config value
     * @return parsed media mode
     */
    public static Optional<AlertToastMediaMode> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return Arrays.stream(values())
                .filter(mode -> mode.identifier.equals(normalized))
                .findFirst();
    }

    /**
     * Parses a media mode and falls back to {@link #DEFAULT} when invalid.
     *
     * @param value command/config value
     * @return parsed or default media mode
     */
    public static AlertToastMediaMode fromString(String value) {
        return parse(value).orElse(DEFAULT);
    }

    /**
     * Returns the supported command suggestions.
     *
     * @return supported ids
     */
    public static String[] ids() {
        return Arrays.stream(values()).map(AlertToastMediaMode::id).toArray(String[]::new);
    }
}
