package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.config.LanguageSetting;

import java.util.Locale;

final class TikTokLanguageResolver {
    private static final String DEFAULT_LANGUAGE = "en-US";
    private static final int LANGUAGE_PARTS = 2;

    private TikTokLanguageResolver() {
    }

    static String resolve(String selectedLanguageCode) {
        String normalizedLocale = LanguageSetting.normalizeLocale(selectedLanguageCode)
                .orElse("");
        if (normalizedLocale.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = normalizedLocale.replace('_', '-');
        String[] parts = normalized.split("-", LANGUAGE_PARTS);
        if (parts.length < LANGUAGE_PARTS || parts[1].isBlank()) {
            return parts[0].toLowerCase(Locale.ROOT);
        }
        return parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
    }
}
