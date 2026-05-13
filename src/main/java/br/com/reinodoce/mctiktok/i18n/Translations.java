package br.com.reinodoce.mctiktok.i18n;

import net.minecraft.network.chat.Component;

/**
 * Translation helper with a plain-text fallback for test and early-client contexts.
 */
public final class Translations {
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
        try {
            return Component.translatable(key, args).getString();
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return fallback(key, args);
        }
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
}
