package br.com.reinodoce.mctiktok.i18n;

import br.com.reinodoce.mctiktok.config.LanguageSetting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Translation helper with a plain-text fallback for test and early-client contexts.
 */
public final class Translations {
    private static final Gson GSON = new Gson();
    private static final String LANGUAGE_RESOURCE_PREFIX = "assets/reinodoce_mctiktok/lang/";
    private static final String LANGUAGE_RESOURCE_SUFFIX = ".json";
    private static final ConcurrentMap<String, Map<String, String>> LANGUAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> EMPTY_LANGUAGE = Map.of();

    private Translations() {
    }

    /**
     * Resolves a translation key to display text.
     *
     * @param key translation key
     * @param args translation arguments
     * @return translated text or a key-plus-arguments fallback
     */
    public static String tr(String key, Object... args) {
        String translated = runtimeTranslation(key, args);
        return translated == null ? trForLocale(LanguageSetting.DEFAULT_LOCALE, key, args) : translated;
    }

    private static String runtimeTranslation(String key, Object... args) {
        try {
            String translated = Component.translatable(key, args).getString();
            return key.equals(translated) ? null : translated;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return null;
        }
    }

    /**
     * Resolves a translation key from a specific mod locale bundle.
     *
     * @param locale locale such as {@code en_us}
     * @param key translation key
     * @param args translation arguments
     * @return translated text, falling back to {@code en_us} or a key-plus-arguments string
     */
    public static String trForLocale(String locale, String key, Object... args) {
        String normalizedLocale = LanguageSetting.normalizeLocale(locale).orElse(LanguageSetting.DEFAULT_LOCALE);
        String pattern = findLocalizedPattern(normalizedLocale, key);
        if (pattern == null) {
            return fallback(key, args);
        }
        return format(pattern, key, args);
    }

    /**
     * Resolves a translation key from Minecraft's runtime language when possible,
     * otherwise from a specific bundled locale.
     *
     * @param locale locale such as {@code en_us}
     * @param useRuntimeLanguage whether to use Minecraft's selected language pipeline
     * @param key translation key
     * @param args translation arguments
     * @return translated text, falling back to {@code en_us} or a key-plus-arguments string
     */
    public static String trForLanguage(String locale, boolean useRuntimeLanguage, String key, Object... args) {
        if (useRuntimeLanguage) {
            String translated = runtimeTranslation(key, args);
            if (translated != null) {
                return translated;
            }
        }
        return trForLocale(locale, key, args);
    }

    private static String fallback(String key, Object[] args) {
        if (args == null || args.length == 0) {
            return key;
        }
        StringBuilder builder = new StringBuilder(key);
        for (Object arg : args) {
            builder.append(' ').append(arg);
        }
        return builder.toString();
    }

    private static String findLocalizedPattern(String locale, String key) {
        String localized = language(locale).get(key);
        if (localized != null || LanguageSetting.DEFAULT_LOCALE.equals(locale)) {
            return localized;
        }
        return language(LanguageSetting.DEFAULT_LOCALE).get(key);
    }

    private static String format(String pattern, String key, Object[] args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(Locale.ROOT, pattern, args);
        } catch (IllegalFormatException exception) {
            return fallback(key, args);
        }
    }

    private static Map<String, String> language(String locale) {
        return LANGUAGE_CACHE.computeIfAbsent(locale, Translations::loadLanguage);
    }

    private static Map<String, String> loadLanguage(String locale) {
        String resourceName = LANGUAGE_RESOURCE_PREFIX + locale + LANGUAGE_RESOURCE_SUFFIX;
        try (InputStream stream = Translations.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                return EMPTY_LANGUAGE;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                return root == null ? EMPTY_LANGUAGE : toMap(root);
            }
        } catch (IOException | JsonIOException | JsonSyntaxException exception) {
            return EMPTY_LANGUAGE;
        }
    }

    private static Map<String, String> toMap(JsonObject root) {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                values.put(entry.getKey(), value.getAsString());
            }
        }
        return Map.copyOf(values);
    }
}
