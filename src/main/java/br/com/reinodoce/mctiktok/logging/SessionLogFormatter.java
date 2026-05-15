package br.com.reinodoce.mctiktok.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formats session log events for the selected output format.
 */
final class SessionLogFormatter {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final DateTimeFormatter EVENT_TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private SessionLogFormatter() {
    }

    static String format(
            SessionLogFormat format,
            Instant timestamp,
            SessionLogEvent event,
            SessionLogPrivacyOptions options
    ) {
        SessionLogPrivacyOptions privacyOptions = options == null
                ? new SessionLogPrivacyOptions(false, false, 0, 0)
                : options;
        if (format == SessionLogFormat.TEXT) {
            return formatText(timestamp, event, privacyOptions);
        }
        return GSON.toJson(toJson(timestamp, event, privacyOptions));
    }

    private static Map<String, Object> toJson(
            Instant timestamp, SessionLogEvent event, SessionLogPrivacyOptions options
    ) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", event.type());
        json.put("timestamp", EVENT_TIMESTAMP_FORMAT.format(timestamp));
        putIfPresent(json, "username", username(event, options));
        if (!options.metadataOnly()) {
            putIfPresent(json, "message", event.message());
        }
        if (event.hasMemberLevel()) {
            json.put("memberLevel", event.memberLevel());
        }
        putIfPresent(json, "giftName", event.giftName());
        if (event.hasCount()) {
            json.put("count", event.count());
        }
        if (event.hasDiamonds()) {
            json.put("diamonds", event.diamonds());
        }
        return json;
    }

    private static String formatText(Instant timestamp, SessionLogEvent event, SessionLogPrivacyOptions options) {
        StringBuilder builder = new StringBuilder()
                .append("timestamp=").append(EVENT_TIMESTAMP_FORMAT.format(timestamp))
                .append('\t').append("type=").append(escapeText(event.type()));
        appendText(builder, "username", username(event, options));
        if (!options.metadataOnly()) {
            appendText(builder, "message", event.message());
        }
        if (event.hasMemberLevel()) {
            builder.append('\t').append("memberLevel=").append(event.memberLevel());
        }
        appendText(builder, "giftName", event.giftName());
        if (event.hasCount()) {
            builder.append('\t').append("count=").append(event.count());
        }
        if (event.hasDiamonds()) {
            builder.append('\t').append("diamonds=").append(event.diamonds());
        }
        return builder.toString();
    }

    private static String username(SessionLogEvent event, SessionLogPrivacyOptions options) {
        if (event.username() == null || event.username().isBlank()) {
            return "";
        }
        return options.anonymized() ? "<redacted-username>" : event.username();
    }

    private static void putIfPresent(Map<String, Object> json, String key, String value) {
        if (value != null && !value.isBlank()) {
            json.put(key, value);
        }
    }

    private static void appendText(StringBuilder builder, String key, String value) {
        if (value != null && !value.isBlank()) {
            builder.append('\t').append(key).append('=').append(escapeText(value));
        }
    }

    private static String escapeText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
