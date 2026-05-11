package br.com.reinodoce.mctiktok.client.overlay;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record InlineMediaMetadata(
        String sourceUrl,
        String kind,
        String contentType,
        long byteSize,
        int width,
        int height,
        long fetchedAt,
        long lastUsedAt,
        String status
) {
    static final String STATUS_READY = "ready";
    static final String STATUS_ERROR = "error";
    static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    static final String DEFAULT_KIND = "unknown";

    private static final Pattern STRING_PATTERN =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");
    private static final String LINE_SEPARATOR = ",\n";
    private static final String QUOTE = "\"";
    private static final String FIELD_SEP = "\": \"";

    static InlineMediaMetadata empty() {
        return new InlineMediaMetadata(
                "", DEFAULT_KIND, DEFAULT_CONTENT_TYPE, 0L, 0, 0, 0L, 0L, STATUS_READY);
    }

    static InlineMediaMetadata read(Path metadataPath) throws IOException {
        if (!Files.exists(metadataPath)) {
            return empty();
        }
        return fromJson(Files.readString(metadataPath, StandardCharsets.UTF_8));
    }

    void writeTo(Path metadataPath) throws IOException {
        Path parent = metadataPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(metadataPath, toJson(), StandardCharsets.UTF_8);
    }

    InlineMediaMetadata withStatus(String newStatus) {
        return new InlineMediaMetadata(
                sourceUrl, kind, contentType, byteSize, width, height, fetchedAt, lastUsedAt, newStatus);
    }

    InlineMediaMetadata withLastUsedAt(long newLastUsedAt) {
        return new InlineMediaMetadata(
                sourceUrl, kind, contentType, byteSize, width, height, fetchedAt, newLastUsedAt, status);
    }

    InlineMediaMetadata withDefaults(String defaultSourceUrl, String defaultKind) {
        return new InlineMediaMetadata(
                isBlank(sourceUrl) ? defaultSourceUrl : sourceUrl,
                isBlank(kind) ? defaultKind : kind,
                isBlank(contentType) ? DEFAULT_CONTENT_TYPE : contentType,
                byteSize,
                width,
                height,
                fetchedAt,
                lastUsedAt,
                isBlank(status) ? STATUS_READY : status);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String toJson() {
        return "{\n"
                + jsonStringField("sourceUrl", sourceUrl) + LINE_SEPARATOR
                + jsonStringField("kind", kind) + LINE_SEPARATOR
                + jsonStringField("contentType", contentType) + LINE_SEPARATOR
                + jsonLongField("byteSize", byteSize) + LINE_SEPARATOR
                + jsonLongField("width", width) + LINE_SEPARATOR
                + jsonLongField("height", height) + LINE_SEPARATOR
                + jsonLongField("fetchedAt", fetchedAt) + LINE_SEPARATOR
                + jsonLongField("lastUsedAt", lastUsedAt) + LINE_SEPARATOR
                + jsonStringField("status", status) + "\n"
                + "}\n";
    }

    private static String jsonStringField(String name, String value) {
        return "  " + QUOTE + name + FIELD_SEP + escape(value) + QUOTE;
    }

    private static String jsonLongField(String name, long value) {
        return "  " + QUOTE + name + "\": " + value;
    }

    private static InlineMediaMetadata fromJson(String json) {
        return new InlineMediaMetadata(
                extractString(json, "sourceUrl", ""),
                extractString(json, "kind", DEFAULT_KIND),
                extractString(json, "contentType", DEFAULT_CONTENT_TYPE),
                extractLong(json, "byteSize", 0L),
                (int) extractLong(json, "width", 0L),
                (int) extractLong(json, "height", 0L),
                extractLong(json, "fetchedAt", 0L),
                extractLong(json, "lastUsedAt", 0L),
                extractString(json, "status", STATUS_READY));
    }

    private static String extractString(String json, String name, String defaultValue) {
        Matcher matcher = STRING_PATTERN.matcher(json);
        while (matcher.find()) {
            if (name.equals(matcher.group(1))) {
                return unescape(matcher.group(2));
            }
        }
        return defaultValue;
    }

    private static long extractLong(String json, String name, long defaultValue) {
        Matcher matcher = NUMBER_PATTERN.matcher(json);
        while (matcher.find()) {
            if (name.equals(matcher.group(1))) {
                try {
                    return Long.parseLong(matcher.group(2));
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static String unescape(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
