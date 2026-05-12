package br.com.reinodoce.mctiktok.rules;

import java.util.Arrays;
import java.util.List;

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

    public static List<String> ids() {
        return Arrays.stream(values())
                .map(GiftComboMode::id)
                .toList();
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
