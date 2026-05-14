package br.com.reinodoce.mctiktok.platform.mc1201;

import br.com.reinodoce.mctiktok.alert.AlertToastMediaMode;
import br.com.reinodoce.mctiktok.alert.AlertToastPayload;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import net.minecraft.resources.ResourceLocation;

/**
 * Converts alert media-mode settings into renderable inline media segments.
 */
final class AlertToastMediaSelector {
    private static final String FALLBACK_TOKEN = "alert";
    private static final String HTTPS_SCHEME = "https://";

    private AlertToastMediaSelector() {
    }

    static Object token(AlertToastPayload payload) {
        if (payload == null || payload.eventType() == null) {
            return FALLBACK_TOKEN;
        }
        return payload.eventType();
    }

    static boolean shouldRenderMedia(AlertToastPayload payload) {
        return payload != null && primarySegment(payload) != null;
    }

    static RichLiveMessage.InlineMediaSegment primarySegment(AlertToastPayload payload) {
        return switch (payload.mediaMode()) {
            case PROFILE -> profileSegment(payload.profileImageUrl());
            case CUSTOM -> customSegment(payload.customImage());
            case PROFILE_CUSTOM -> firstNonNull(
                    profileSegment(payload.profileImageUrl()),
                    customSegment(payload.customImage()));
            case GIFT -> giftSegment(payload.giftImageUrl());
            case INLINE -> firstNonNull(
                    profileSegment(payload.profileImageUrl()),
                    giftSegment(payload.giftImageUrl()),
                    customSegment(payload.customImage()));
            case NONE -> null;
        };
    }

    static RichLiveMessage.InlineMediaSegment overlaySegment(AlertToastPayload payload) {
        if (payload.mediaMode() != AlertToastMediaMode.PROFILE_CUSTOM || payload.profileImageUrl().isBlank()) {
            return null;
        }
        return customSegment(payload.customImage());
    }

    static String normalizeMediaSource(String source) {
        String trimmed = source == null ? "" : source.trim();
        if (trimmed.isBlank()
                || InlineMediaUrls.isResourceUrl(trimmed)
                || trimmed.regionMatches(true, 0, HTTPS_SCHEME, 0, HTTPS_SCHEME.length())) {
            return trimmed;
        }
        ResourceLocation location = ResourceLocation.tryParse(trimmed);
        if (location == null || !trimmed.contains(":")) {
            return trimmed;
        }
        return InlineMediaUrls.resourceUrl(location.getNamespace(), location.getPath());
    }

    private static RichLiveMessage.InlineMediaSegment profileSegment(String source) {
        String normalized = normalizeMediaSource(source);
        return normalized.isBlank() ? null : new RichLiveMessage.AvatarSegment(normalized, "");
    }

    private static RichLiveMessage.InlineMediaSegment giftSegment(String source) {
        String normalized = normalizeMediaSource(source);
        return normalized.isBlank() ? null : new RichLiveMessage.GiftIconSegment(normalized, normalized, "");
    }

    private static RichLiveMessage.InlineMediaSegment customSegment(String source) {
        String normalized = normalizeMediaSource(source);
        return normalized.isBlank() ? null : new RichLiveMessage.RemoteEmoteSegment(normalized, normalized, "");
    }

    private static RichLiveMessage.InlineMediaSegment firstNonNull(
            RichLiveMessage.InlineMediaSegment first,
            RichLiveMessage.InlineMediaSegment second
    ) {
        return first == null ? second : first;
    }

    private static RichLiveMessage.InlineMediaSegment firstNonNull(
            RichLiveMessage.InlineMediaSegment first,
            RichLiveMessage.InlineMediaSegment second,
            RichLiveMessage.InlineMediaSegment third
    ) {
        RichLiveMessage.InlineMediaSegment selected = firstNonNull(first, second);
        return selected == null ? third : selected;
    }
}
