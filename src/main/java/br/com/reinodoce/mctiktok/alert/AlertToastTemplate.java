package br.com.reinodoce.mctiktok.alert;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation helper for user-configured alert toast templates.
 */
public final class AlertToastTemplate {
    /** Blank value means "use the built-in localized text". */
    public static final String DEFAULT = "";
    private static final int MAX_TEMPLATE_LENGTH = 240;
    private static final int MAX_BRACE_DEPTH = 1;
    private static final int UNBALANCED_BRACE_DEPTH = 0;
    private static final char OPEN_TOKEN = '{';
    private static final char CLOSE_TOKEN = '}';
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([^{}]+)}");
    private static final Set<String> SUPPORTED_TOKENS = Set.of(
            "username",
            "giftName",
            "count",
            "diamonds",
            "memberLevel",
            "profileImage",
            "giftImage"
    );

    private AlertToastTemplate() {
    }

    /**
     * Sanitizes a persisted template, falling back to default when invalid.
     *
     * @param value template value
     * @return sanitized template, or blank default
     */
    public static String sanitize(String value) {
        String cleaned = clean(value);
        return isValid(cleaned) ? cleaned : DEFAULT;
    }

    /**
     * Reports whether a non-blank template uses only supported tokens.
     *
     * @param value template value
     * @return true when valid
     */
    public static boolean isValid(String value) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            return false;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(cleaned);
        while (matcher.find()) {
            if (!SUPPORTED_TOKENS.contains(matcher.group(1))) {
                return false;
            }
        }
        return !hasUnmatchedBrace(cleaned);
    }

    /**
     * Returns supported template tokens for documentation or diagnostics.
     *
     * @return supported token names
     */
    public static Set<String> tokens() {
        return SUPPORTED_TOKENS;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.strip();
        StringBuilder builder = new StringBuilder(Math.min(trimmed.length(), MAX_TEMPLATE_LENGTH));
        for (int index = 0; index < trimmed.length() && builder.length() < MAX_TEMPLATE_LENGTH; index++) {
            char current = trimmed.charAt(index);
            if (!Character.isISOControl(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static boolean hasUnmatchedBrace(String value) {
        int balanced = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == OPEN_TOKEN) {
                balanced++;
            } else if (current == CLOSE_TOKEN) {
                balanced--;
            }
            if (balanced < UNBALANCED_BRACE_DEPTH || balanced > MAX_BRACE_DEPTH) {
                return true;
            }
        }
        return balanced != UNBALANCED_BRACE_DEPTH;
    }
}
