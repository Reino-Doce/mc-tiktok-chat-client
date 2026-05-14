package br.com.reinodoce.mctiktok.chat;

/**
 * Encodes whether fixed synthetic text should use Minecraft runtime translations.
 */
final class FixedTextLanguage {
    private static final String RUNTIME_LANGUAGE_PREFIX = "runtime:";

    private FixedTextLanguage() {
    }

    static String runtime(String language) {
        return RUNTIME_LANGUAGE_PREFIX + (language == null ? "" : language);
    }

    static boolean usesRuntime(String language) {
        return language == null || language.isBlank() || language.startsWith(RUNTIME_LANGUAGE_PREFIX);
    }

    static String locale(String language) {
        return language != null && language.startsWith(RUNTIME_LANGUAGE_PREFIX)
                ? language.substring(RUNTIME_LANGUAGE_PREFIX.length())
                : language;
    }
}
