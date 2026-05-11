package br.com.reinodoce.mctiktok.emoji;

public final class TwemojiUrlResolver {
    public static final String BASE_URL = "https://cdn.jsdelivr.net/gh/jdecked/twemoji@17.0.2/assets/72x72/";

    private static final int VARIATION_SELECTOR_15 = 0xFE0E;
    private static final int VARIATION_SELECTOR_16 = 0xFE0F;

    private TwemojiUrlResolver() {
    }

    public static String toIconId(int[] codePoints) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int codePoint : codePoints) {
            if (codePoint == VARIATION_SELECTOR_15 || codePoint == VARIATION_SELECTOR_16) {
                continue;
            }
            if (!first) {
                builder.append('-');
            }
            builder.append(Integer.toHexString(codePoint));
            first = false;
        }
        return builder.toString();
    }

    public static String toUrl(String iconId) {
        if (iconId == null || iconId.isBlank()) {
            return "";
        }
        return BASE_URL + iconId + ".png";
    }
}
