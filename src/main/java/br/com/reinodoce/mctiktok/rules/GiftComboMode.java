package br.com.reinodoce.mctiktok.rules;

public enum GiftComboMode {
    IGNORE("ignore"),
    SINGLE("single"),
    BULK("bulk");

    private final String id;

    GiftComboMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static GiftComboMode fromString(String value) {
        if (value == null) {
            return BULK;
        }
        for (GiftComboMode mode : values()) {
            if (mode.id.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return BULK;
    }
}
