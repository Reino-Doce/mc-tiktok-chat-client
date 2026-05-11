package br.com.reinodoce.mctiktok.tiktok;

final class MessageTextUtils {
    static final int MAX_SANITIZED_LENGTH = 256;

    private static final int OBJECT_REPLACEMENT_CHARACTER = 0xFFFC;
    private static final int REPLACEMENT_CHARACTER = 0xFFFD;
    private static final int ZERO_WIDTH_JOINER = 0x200D;
    private static final int VARIATION_SELECTOR_15 = 0xFE0E;
    private static final int VARIATION_SELECTOR_16 = 0xFE0F;
    private static final int COMBINING_ENCLOSING_KEYCAP = 0x20E3;

    private MessageTextUtils() {
    }

    static boolean isPlaceholderCodePoint(int codePoint) {
        if (codePoint == OBJECT_REPLACEMENT_CHARACTER || codePoint == REPLACEMENT_CHARACTER) {
            return true;
        }
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.PRIVATE_USE_AREA
                || block == Character.UnicodeBlock.SUPPLEMENTARY_PRIVATE_USE_AREA_A
                || block == Character.UnicodeBlock.SUPPLEMENTARY_PRIVATE_USE_AREA_B;
    }

    static boolean isPlaceholderDecorator(int codePoint) {
        return codePoint == ZERO_WIDTH_JOINER
                || codePoint == VARIATION_SELECTOR_15
                || codePoint == VARIATION_SELECTOR_16
                || codePoint == COMBINING_ENCLOSING_KEYCAP;
    }

    static int skipPlaceholderCodePoints(int[] codePoints, int index) {
        if (index >= codePoints.length || !isPlaceholderCodePoint(codePoints[index])) {
            return index;
        }
        int cursor = index + 1;
        while (cursor < codePoints.length && isPlaceholderDecorator(codePoints[cursor])) {
            cursor++;
        }
        return cursor;
    }

    static String codePointsToString(int[] codePoints, int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) {
            return "";
        }
        return new String(codePoints, startInclusive, endExclusive - startInclusive);
    }

    static String sanitizeSegmentText(String rawText) {
        if (rawText == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(rawText.length());
        rawText.codePoints().forEach(codePoint -> appendIfPrintable(sanitized, codePoint));
        String collapsed = sanitized.toString().replaceAll("\\s+", " ");
        return collapsed.length() > MAX_SANITIZED_LENGTH
                ? collapsed.substring(0, MAX_SANITIZED_LENGTH)
                : collapsed;
    }

    private static void appendIfPrintable(StringBuilder sink, int codePoint) {
        if (codePoint == 0 || isPlaceholderCodePoint(codePoint)) {
            return;
        }
        if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
            return;
        }
        sink.appendCodePoint(codePoint);
    }
}
