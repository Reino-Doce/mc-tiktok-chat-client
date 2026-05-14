package br.com.reinodoce.mctiktok.config;

import java.util.Arrays;
import java.util.Locale;

/**
 * Screen anchors for the local LIVE HUD.
 */
public enum HudPosition {
    /** Top-left HUD anchor. */
    TOP_LEFT("top-left"),
    /** Top-right HUD anchor. */
    TOP_RIGHT("top-right"),
    /** Bottom-left HUD anchor. */
    BOTTOM_LEFT("bottom-left"),
    /** Bottom-right HUD anchor. */
    BOTTOM_RIGHT("bottom-right");

    private final String identifier;

    HudPosition(String id) {
        this.identifier = id;
    }

    /**
     * Returns the persisted identifier.
     *
     * @return position identifier
     */
    public String id() {
        return identifier;
    }

    /**
     * Parses a HUD position identifier.
     *
     * @param value user or config value
     * @return parsed position, or {@link #TOP_LEFT} when unknown
     */
    public static HudPosition fromString(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (HudPosition position : values()) {
            if (position.id().equals(normalized)) {
                return position;
            }
        }
        return TOP_LEFT;
    }

    /**
     * Returns command suggestion identifiers.
     *
     * @return position identifiers
     */
    public static Iterable<String> ids() {
        return Arrays.stream(values()).map(HudPosition::id).toList();
    }
}
