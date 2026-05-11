package br.com.reinodoce.mctiktok.client.overlay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class InlineMediaCacheKeys {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String HEX_FORMAT = "%02x";

    private InlineMediaCacheKeys() {
    }

    static String cacheKey(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format(HEX_FORMAT, value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash inline media URL", exception);
        }
    }
}
