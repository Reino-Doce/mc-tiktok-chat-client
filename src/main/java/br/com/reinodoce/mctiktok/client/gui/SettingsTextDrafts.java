package br.com.reinodoce.mctiktok.client.gui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Keeps raw text-field drafts separate from sanitized persisted config values until the GUI is saved.
 */
final class SettingsTextDrafts {
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, Consumer<String>> appliers = new LinkedHashMap<>();

    String value(String key, Supplier<String> fallback, Consumer<String> applier) {
        appliers.put(key, applier);
        return values.containsKey(key) ? values.get(key) : fallback.get();
    }

    void update(String key, String value) {
        values.put(key, value == null ? "" : value);
    }

    void apply() {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            Consumer<String> applier = appliers.get(entry.getKey());
            if (applier != null) {
                applier.accept(entry.getValue());
            }
        }
    }
}
