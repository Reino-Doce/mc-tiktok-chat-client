package br.com.reinodoce.mctiktok.client.font;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allocates private-use Unicode tokens for rich-message inline media segments.
 */
public final class InlineMediaTokenRegistry {
    /** First supplementary private-use code point reserved for inline media. */
    public static final int MIN_CODE_POINT = 0xF0000;
    /** Last supplementary private-use code point reserved for inline media. */
    public static final int MAX_CODE_POINT = 0xFFFFD;
    private static final float CHAT_MEDIA_HEIGHT = 9.0F;

    private final InlineMediaCache mediaCache;
    private final AtomicInteger nextCodePoint = new AtomicInteger(MIN_CODE_POINT);
    private final Map<String, TokenEntry> entriesByKey = new ConcurrentHashMap<>();
    private final Map<Integer, TokenEntry> entriesByCodePoint = new ConcurrentHashMap<>();

    /**
     * Creates a registry backed by the media cache used during rendering.
     *
     * @param mediaCache cache that resolves token media handles
     */
    public InlineMediaTokenRegistry(InlineMediaCache mediaCache) {
        this.mediaCache = mediaCache;
    }

    /**
     * Returns a stable token string for a segment, allocating one if needed.
     *
     * @param segment inline media segment
     * @return token string containing one supplementary private-use code point
     */
    public String tokenFor(RichLiveMessage.InlineMediaSegment segment) {
        return entryFor(segment).token();
    }

    /**
     * Looks up a token entry by private-use code point.
     *
     * @param codePoint private-use code point
     * @return token entry or {@code null} when the code point is unknown
     */
    public TokenEntry lookup(int codePoint) {
        return entriesByCodePoint.get(codePoint);
    }

    /**
     * Returns an inline media advance or a negative sentinel for normal glyph handling.
     *
     * @param codePoint code point to inspect
     * @return inline media advance, or {@code -1.0F} when the code point is not registered
     */
    public float inlineAdvanceOrSentinel(int codePoint) {
        TokenEntry entry = lookup(codePoint);
        if (entry == null) {
            return -1.0F;
        }
        return advanceFor(entry.segment());
    }

    /**
     * Resolves the texture handle for a token code point.
     *
     * @param codePoint registered private-use code point
     * @return media texture handle or an error handle for unknown tokens
     */
    public InlineMediaCache.TextureHandle resolveHandle(int codePoint) {
        TokenEntry entry = lookup(codePoint);
        if (entry == null) {
            return InlineMediaCache.TextureHandle.error(
                    InlineMediaCache.ERROR_TEXTURE,
                    InlineMediaCache.FALLBACK_DIMENSION,
                    InlineMediaCache.FALLBACK_DIMENSION);
        }
        return mediaCache.resolve(entry.segment());
    }

    /**
     * Computes the chat-line advance for a media segment.
     *
     * @param segment inline media segment
     * @return render advance in font pixels
     */
    public float advanceFor(RichLiveMessage.InlineMediaSegment segment) {
        if (segment.renderStyle() == RichLiveMessage.InlineMediaRenderStyle.SQUARE_CROP) {
            return CHAT_MEDIA_HEIGHT;
        }

        InlineMediaCache.Dimensions dimensions = mediaCache.dimensionsFor(segment);
        int width = Math.max(1, dimensions.width());
        int height = Math.max(1, dimensions.height());
        return CHAT_MEDIA_HEIGHT * width / (float) height;
    }

    /**
     * Returns the number of allocated token entries.
     *
     * @return registered token count
     */
    public int size() {
        return entriesByKey.size();
    }

    /**
     * Checks whether a code point is in the reserved range and currently registered.
     *
     * @param codePoint code point to inspect
     * @return true when the code point belongs to this registry
     */
    public boolean isTokenCodePoint(int codePoint) {
        return codePoint >= MIN_CODE_POINT && codePoint <= MAX_CODE_POINT && entriesByCodePoint.containsKey(codePoint);
    }

    private TokenEntry entryFor(RichLiveMessage.InlineMediaSegment segment) {
        return entriesByKey.computeIfAbsent(keyFor(segment), ignored -> createEntry(segment));
    }

    private TokenEntry createEntry(RichLiveMessage.InlineMediaSegment segment) {
        int codePoint = nextCodePoint.getAndIncrement();
        if (codePoint > MAX_CODE_POINT) {
            throw new IllegalStateException("Inline media token registry exhausted the supplementary PUA range");
        }

        TokenEntry entry = new TokenEntry(codePoint, new String(Character.toChars(codePoint)), segment);
        entriesByCodePoint.put(codePoint, entry);
        return entry;
    }

    private String keyFor(RichLiveMessage.InlineMediaSegment segment) {
        return segment.kind().id()
                + "|" + segment.renderStyle().name()
                + "|" + segment.sourceKey()
                + "|" + segment.sourceUrl();
    }

    /**
     * Registered token binding.
     *
     * @param codePoint allocated private-use code point
     * @param token Java string containing the code point
     * @param segment segment associated with the token
     */
    public record TokenEntry(int codePoint, String token, RichLiveMessage.InlineMediaSegment segment) {
    }
}
