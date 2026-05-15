package br.com.reinodoce.mctiktok.diagnostics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sanitizes caller-provided client diagnostics before export.
 */
final class DiagnosticsClientReport {
    private DiagnosticsClientReport() {
    }

    static Map<String, Object> from(Map<String, Object> client) {
        if (client == null || client.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> output = ordered();
        for (Map.Entry<String, Object> entry : client.entrySet()) {
            putSanitizedEntry(output, entry.getKey(), entry.getValue());
        }
        return output;
    }

    private static Object sanitizedValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> output = ordered();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                putSanitizedEntry(output, String.valueOf(entry.getKey()), entry.getValue());
            }
            return output;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> output = new ArrayList<>();
            for (Object item : collection) {
                output.add(sanitizedValue(item));
            }
            return output;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return value == null ? "" : DiagnosticsSanitizer.text(String.valueOf(value));
    }

    private static void putSanitizedEntry(Map<String, Object> output, String key, Object value) {
        String safeKey = DiagnosticsSanitizer.text(key);
        Object safeValue = DiagnosticsSanitizer.isSensitiveKey(key)
                ? DiagnosticsSanitizer.REDACTED
                : sanitizedValue(value);
        output.put(safeKey, safeValue);
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
