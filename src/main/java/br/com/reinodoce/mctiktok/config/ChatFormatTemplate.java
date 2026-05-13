package br.com.reinodoce.mctiktok.config;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation helpers for the persisted chat format template.
 */
public final class ChatFormatTemplate {
    /** Token replaced with the configured LIVE prefix. */
    public static final String PREFIX_TOKEN = "{prefix}";
    /** Token replaced with the display username. */
    public static final String USERNAME_TOKEN = "{username}";
    /** Token replaced with the message body. */
    public static final String MESSAGE_TOKEN = "{message}";

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{[^}]+}");
    private static final Set<String> SUPPORTED_TOKENS = Set.of(PREFIX_TOKEN, USERNAME_TOKEN, MESSAGE_TOKEN);

    private ChatFormatTemplate() {
    }

    /**
     * Returns a safe template, falling back to the default when invalid.
     *
     * @param raw template from config or command input
     * @return valid template
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return ReinodoceConfig.DEFAULT_CHAT_FORMAT;
        }
        String trimmed = raw.trim();
        return isValid(trimmed) ? trimmed : ReinodoceConfig.DEFAULT_CHAT_FORMAT;
    }

    /**
     * Reports whether a template uses only supported tokens and includes all required fields.
     *
     * @param template candidate template
     * @return true when the template is usable
     */
    public static boolean isValid(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        if (!hasRequiredTokens(template)) {
            return false;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        while (matcher.find()) {
            if (!SUPPORTED_TOKENS.contains(matcher.group())) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasRequiredTokens(String template) {
        return template.contains(PREFIX_TOKEN)
                && template.contains(USERNAME_TOKEN)
                && template.contains(MESSAGE_TOKEN);
    }
}
