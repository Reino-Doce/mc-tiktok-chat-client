package br.com.reinodoce.mctiktok.rules;

public enum GiftComboMode {
    IGNORE("ignore"),
    SINGLE("single"),
    BULK("bulk");

    private final String identifier;

    GiftComboMode(String identifier) {
        this.identifier = identifier;
    }

    public String id() {
        return identifier;
    }

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
