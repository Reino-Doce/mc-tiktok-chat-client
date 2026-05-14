package br.com.reinodoce.mctiktok.alert;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders alert toast templates with stable TikTok LIVE event tokens.
 */
public final class AlertToastTemplateRenderer {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([^{}]+)}");

    private AlertToastTemplateRenderer() {
    }

    /**
     * Renders a configured template, falling back to the event default when blank or invalid.
     *
     * @param template configured template
     * @param values token values
     * @param fallback default localized message
     * @return rendered message
     */
    public static String render(String template, Map<String, String> values, String fallback) {
        String safeFallback = fallback == null ? "" : fallback;
        String sanitized = AlertToastTemplate.sanitize(template);
        if (sanitized.isBlank()) {
            return safeFallback;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(sanitized);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String replacement = values == null ? "" : values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        String result = rendered.toString().trim();
        return result.isBlank() ? safeFallback : result;
    }
}
