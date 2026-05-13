package br.com.reinodoce.mctiktok.rules;

import java.util.Arrays;
import java.util.List;

/**
 * Operator policy for TikTok gift combo events.
 */
public enum GiftComboMode {
    /** Suppresses gift combo output. */
    IGNORE("ignore"),
    /** Emits each gift event as it arrives. */
    SINGLE("single"),
    /** Collapses a combo into one bulk output. */
    BULK("bulk");

    private final String identifier;

    GiftComboMode(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the persisted configuration identifier.
     *
     * @return stable lowercase identifier
     */
    public String id() {
        return identifier;
    }

    /**
     * Lists all valid persisted identifiers for suggestions and docs.
     *
     * @return valid identifiers
     */
    public static List<String> ids() {
        return Arrays.stream(values())
                .map(GiftComboMode::id)
                .toList();
    }

    /**
     * Resolves a configured identifier, defaulting to {@link #BULK} for blank or unknown values.
     *
     * @param value configured value
     * @return resolved mode
     */
    public static GiftComboMode fromString(String value) {
        if (value == null) {
            return BULK;
        }
        for (GiftComboMode mode : values()) {
            if (mode.identifier.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return BULK;
    }
}
