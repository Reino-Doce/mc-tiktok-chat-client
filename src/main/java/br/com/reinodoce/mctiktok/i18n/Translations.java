package br.com.reinodoce.mctiktok.i18n;

import net.minecraft.network.chat.Component;

public final class Translations {
    private Translations() {
    }

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
