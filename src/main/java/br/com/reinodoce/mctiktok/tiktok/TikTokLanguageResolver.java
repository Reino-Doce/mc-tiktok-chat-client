package br.com.reinodoce.mctiktok.tiktok;

import java.util.Locale;

final class TikTokLanguageResolver {
    private static final String DEFAULT_LANGUAGE = "en-US";
    private static final int LANGUAGE_PARTS = 2;

    private TikTokLanguageResolver() {
    }

    static String resolve(String selectedLanguageCode) {
        if (selectedLanguageCode == null || selectedLanguageCode.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = selectedLanguageCode.trim().replace('_', '-');
        String[] parts = normalized.split("-", LANGUAGE_PARTS);
        if (parts.length < LANGUAGE_PARTS || parts[1].isBlank()) {
            return parts[0].toLowerCase(Locale.ROOT);
        }
        return parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
    }
}
