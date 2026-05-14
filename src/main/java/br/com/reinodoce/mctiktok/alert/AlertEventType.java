package br.com.reinodoce.mctiktok.alert;

/**
 * TikTok LIVE event types that can produce local client alerts.
 */
public enum AlertEventType {
    /** Gift alert settings. */
    GIFT("gift"),
    /** Follow alert settings. */
    FOLLOW("follow"),
    /** Join alert settings. */
    JOIN("join"),
    /** Member-level alert settings. */
    MEMBER_LEVEL("member-level");

    private final String identifier;

    AlertEventType(String id) {
        this.identifier = id;
    }

    /**
     * Returns the public command/config identifier.
     *
     * @return event type identifier
     */
    public String id() {
        return identifier;
    }
}
