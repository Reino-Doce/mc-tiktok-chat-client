package br.com.reinodoce.mctiktok.alert;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validation rules for persisted and command-provided alert sound identifiers.
 */
public final class AlertSoundId {
    /** Persisted literal that selects the built-in default alert sound. */
    public static final String DEFAULT = "default";
    /** Vanilla sound played when the persisted identifier is {@link #DEFAULT}. */
    public static final String DEFAULT_SOUND_RESOURCE = "minecraft:ui.toast.in";

    private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    private AlertSoundId() {
    }

    /**
     * Normalizes a sound identifier for persistence, falling back to {@link #DEFAULT} when invalid.
     *
     * @param soundId candidate sound identifier
     * @return normalized sound identifier
     */
    public static String sanitize(String soundId) {
        String normalized = normalize(soundId);
        return isValid(normalized) ? normalized : DEFAULT;
    }

    /**
     * Reports whether a sound identifier is accepted by commands and config.
     *
     * @param soundId candidate sound identifier
     * @return true when the identifier is {@code default} or a namespaced resource id
     */
    public static boolean isValid(String soundId) {
        return DEFAULT.equals(soundId) || RESOURCE_LOCATION.matcher(soundId).matches();
    }

    /**
     * Converts a persisted identifier to the sound resource location that should be played.
     *
     * @param soundId persisted sound identifier
     * @return concrete sound resource id
     */
    public static String soundResource(String soundId) {
        String sanitized = sanitize(soundId);
        return DEFAULT.equals(sanitized) ? DEFAULT_SOUND_RESOURCE : sanitized;
    }

    /**
     * Trims and lowercases a candidate identifier.
     *
     * @param soundId candidate sound identifier
     * @return normalized candidate or blank
     */
    public static String normalize(String soundId) {
        return soundId == null ? "" : soundId.trim().toLowerCase(Locale.ROOT);
    }
}
