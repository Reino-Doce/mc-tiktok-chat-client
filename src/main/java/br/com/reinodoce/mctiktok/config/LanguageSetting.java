package br.com.reinodoce.mctiktok.config;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normalizes the persisted language setting and resolves the effective locale.
 */
public final class LanguageSetting {
    /** Automatic language detection from the Minecraft client. */
    public static final String AUTO = "auto";
    /** Baseline locale used when Minecraft does not provide a valid locale. */
    public static final String DEFAULT_LOCALE = "en_us";
    private static final Pattern LOCALE_PATTERN =
            Pattern.compile("^[a-zA-Z]{2,3}[-_](?:[a-zA-Z]{2}|\\d{3})$");
    private static final List<String> COMMON_SETTINGS = List.of(
            AUTO,
            "ar_sa",
            "de_de",
            "en_us",
            "es_mx",
            "fil_ph",
            "fr_fr",
            "id_id",
            "ja_jp",
            "pt_br",
            "th_th",
            "tr_tr",
            "vi_vn");

    private LanguageSetting() {
    }

    /**
     * Parses a command/config setting value.
     *
     * @param value raw setting
     * @return normalized {@code auto} or locale when valid
     */
    public static Optional<String> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if (AUTO.equalsIgnoreCase(trimmed)) {
            return Optional.of(AUTO);
        }
        return normalizeLocale(trimmed);
    }

    /**
     * Sanitizes a persisted setting, falling back to {@code auto}.
     *
     * @param value raw setting
     * @return normalized setting
     */
    public static String sanitize(String value) {
        return parse(value).orElse(AUTO);
    }

    /**
     * Normalizes a locale code to Minecraft's lowercase underscore form.
     *
     * @param value raw locale
     * @return normalized locale when syntactically valid
     */
    public static Optional<String> normalizeLocale(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if (!LOCALE_PATTERN.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        return Optional.of(trimmed.replace('-', '_').toLowerCase(Locale.ROOT));
    }

    /**
     * Resolves the locale actively used by the mod.
     *
     * @param configuredLanguage persisted language setting
     * @param minecraftLanguage current Minecraft client locale
     * @return override locale, Minecraft locale, or {@code en_us}
     */
    public static String resolveEffective(String configuredLanguage, String minecraftLanguage) {
        String setting = sanitize(configuredLanguage);
        if (!AUTO.equals(setting)) {
            return setting;
        }
        return normalizeLocale(minecraftLanguage).orElse(DEFAULT_LOCALE);
    }

    /**
     * Reports whether a normalized setting is automatic.
     *
     * @param value setting value
     * @return true when the setting is {@code auto}
     */
    public static boolean isAuto(String value) {
        return AUTO.equals(sanitize(value));
    }

    /**
     * Returns command suggestions for common bundled locales.
     *
     * @return suggested setting values
     */
    public static Iterable<String> suggestions() {
        return COMMON_SETTINGS;
    }
}
