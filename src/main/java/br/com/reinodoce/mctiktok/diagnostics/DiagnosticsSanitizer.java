package br.com.reinodoce.mctiktok.diagnostics;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts values that are unsafe for support diagnostics.
 */
public final class DiagnosticsSanitizer {
    /** Generic redaction marker used in exported diagnostics. */
    public static final String REDACTED = "<redacted>";
    /** Username redaction marker used in exported diagnostics. */
    public static final String REDACTED_USERNAME = "<redacted-username>";

    private static final String HOME_PATH = "<home>";
    private static final String REDACTED_HOST = "<redacted-host>";
    private static final String REDACTED_QUERY = "?<redacted>";
    private static final Pattern AUTH_VALUE = Pattern.compile("(?i)\\b(Bearer|Basic)\\s+[^\\s,;]+");
    private static final Pattern CONTROL_CHARACTER = Pattern.compile("\\p{Cntrl}");
    private static final String SENSITIVE_KEY_STEM =
            "password|passwd|token|secret|credential|cookie|session|authorization|auth|api[_-]?key";
    private static final Pattern SENSITIVE_KEY_NAME = Pattern.compile(
            "(?i).*(?:" + SENSITIVE_KEY_STEM + ").*");
    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?i)\\b([A-Za-z0-9_.-]*(?:" + SENSITIVE_KEY_STEM + ")"
                    + "[A-Za-z0-9_.-]*)\\s*([:=])\\s*[^\\s,;]+");
    private static final Pattern URL = Pattern.compile("https?://[^\\s\"'<>]+");
    private static final Pattern USERNAME_MENTION =
            Pattern.compile("(?i)(?<![A-Za-z0-9._])@[A-Za-z0-9._]{2,30}(?![A-Za-z0-9._])");
    private static final Pattern UNIX_HOME_PATH =
            Pattern.compile("(?i)(?<!\\w)/(?:home|Users)/[^/\\s\"'<>]+(?:/[^\\s\"'<>]+)*");
    private static final Pattern WINDOWS_HOME_PATH = Pattern.compile(
            "(?i)\\b[A-Z]:[\\\\/]Users[\\\\/][^\\\\/\\s\"'<>]+(?:[\\\\/][^\\s\"'<>]+)*");

    private DiagnosticsSanitizer() {
    }

    /**
     * Redacts a username field.
     *
     * @param username username value
     * @return blank when absent, otherwise a redaction marker
     */
    public static String username(String username) {
        return username == null || username.isBlank() ? "" : REDACTED_USERNAME;
    }

    /**
     * Sanitizes free-form diagnostic text.
     *
     * @param value free-form value
     * @return sanitized value
     */
    public static String text(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = CONTROL_CHARACTER.matcher(value.trim()).replaceAll(" ");
        sanitized = scrubUrls(sanitized);
        sanitized = SENSITIVE_KEY_VALUE.matcher(sanitized).replaceAll("$1$2" + REDACTED);
        sanitized = AUTH_VALUE.matcher(sanitized).replaceAll("$1 " + REDACTED);
        sanitized = USERNAME_MENTION.matcher(sanitized).replaceAll("@" + REDACTED_USERNAME);
        return scrubHomePaths(sanitized).trim();
    }

    /**
     * Sanitizes free-form diagnostic text and redacts known usernames even when they appear without {@code @}.
     *
     * @param value free-form value
     * @param knownUsernames usernames known to the current session/config
     * @return sanitized value
     */
    public static String text(String value, Collection<String> knownUsernames) {
        String sanitized = text(value);
        String usernamePattern = knownUsernamePattern(knownUsernames);
        if (usernamePattern.isBlank()) {
            return sanitized;
        }
        return Pattern.compile(usernamePattern, Pattern.CASE_INSENSITIVE)
                .matcher(sanitized)
                .replaceAll(REDACTED_USERNAME);
    }

    /**
     * Sanitizes a URL, resource identifier, or local path-like value.
     *
     * @param value configured resource value
     * @param knownUsernames usernames known to the current session/config
     * @return sanitized value
     */
    public static String resource(String value, Collection<String> knownUsernames) {
        return text(value, knownUsernames);
    }

    /**
     * Sanitizes a local path for diagnostics.
     *
     * @param path path value
     * @return sanitized path text
     */
    public static String path(Path path) {
        return path == null ? "" : text(path.normalize().toString());
    }

    static boolean isSensitiveKey(String key) {
        return key != null && SENSITIVE_KEY_NAME.matcher(key).matches();
    }

    private static String scrubUrls(String value) {
        Matcher matcher = URL.matcher(value);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(sanitizeUrl(matcher.group())));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String sanitizeUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return uri.getScheme() + "://" + REDACTED_HOST;
            }
            return sanitizedUrl(uri, host);
        } catch (IllegalArgumentException exception) {
            return "https://" + REDACTED_HOST;
        }
    }

    private static String sanitizedUrl(URI uri, String host) {
        StringBuilder builder = new StringBuilder(uri.getScheme()).append("://").append(host);
        if (uri.getPort() >= 0) {
            builder.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null && !uri.getRawPath().isBlank()) {
            builder.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null) {
            builder.append(REDACTED_QUERY);
        }
        return builder.toString();
    }

    private static String scrubHomePaths(String value) {
        String sanitized = WINDOWS_HOME_PATH.matcher(value).replaceAll(HOME_PATH);
        return UNIX_HOME_PATH.matcher(sanitized).replaceAll(HOME_PATH);
    }

    private static String knownUsernamePattern(Collection<String> knownUsernames) {
        if (knownUsernames == null || knownUsernames.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("|", "(?<![A-Za-z0-9._])@?(", ")(?![A-Za-z0-9._])");
        boolean found = false;
        for (String username : knownUsernames) {
            String normalized = normalizeUsername(username);
            if (!normalized.isBlank()) {
                joiner.add(Pattern.quote(normalized));
                found = true;
            }
        }
        return found ? joiner.toString() : "";
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        String normalized = username.trim();
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }
}
