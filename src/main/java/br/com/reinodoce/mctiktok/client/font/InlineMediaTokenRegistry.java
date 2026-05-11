package br.com.reinodoce.mctiktok.client.font;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class InlineMediaTokenRegistry {
    public static final int MIN_CODE_POINT = 0xF0000;
    public static final int MAX_CODE_POINT = 0xFFFFD;
    private static final float CHAT_MEDIA_HEIGHT = 9.0F;

    private final InlineMediaCache mediaCache;
    private final AtomicInteger nextCodePoint = new AtomicInteger(MIN_CODE_POINT);
    private final Map<String, TokenEntry> entriesByKey = new ConcurrentHashMap<>();
    private final Map<Integer, TokenEntry> entriesByCodePoint = new ConcurrentHashMap<>();

    public InlineMediaTokenRegistry(InlineMediaCache mediaCache) {
        this.mediaCache = mediaCache;
    }

    public String tokenFor(RichLiveMessage.InlineMediaSegment segment) {
        return entryFor(segment).token();
    }

    public TokenEntry lookup(int codePoint) {
        return entriesByCodePoint.get(codePoint);
    }

    public float inlineAdvanceOrSentinel(int codePoint) {
        TokenEntry entry = lookup(codePoint);
        if (entry == null) {
            return -1.0F;
        }
        return advanceFor(entry.segment());
    }

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

    public float advanceFor(RichLiveMessage.InlineMediaSegment segment) {
        if (segment.renderStyle() == RichLiveMessage.InlineMediaRenderStyle.SQUARE_CROP) {
            return CHAT_MEDIA_HEIGHT;
        }

        InlineMediaCache.Dimensions dimensions = mediaCache.dimensionsFor(segment);
        int width = Math.max(1, dimensions.width());
        int height = Math.max(1, dimensions.height());
        return CHAT_MEDIA_HEIGHT * width / (float) height;
    }

    public int size() {
        return entriesByKey.size();
    }

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

    public record TokenEntry(int codePoint, String token, RichLiveMessage.InlineMediaSegment segment) {
    }
}
