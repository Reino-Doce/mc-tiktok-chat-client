package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlineMediaCacheTest {

    @Test
    void avatarUsesDefaultUserImageForLoadingAndErrorPlaceholders() {
        RichLiveMessage.AvatarSegment avatar = new RichLiveMessage.AvatarSegment("https://cdn.example/avatar.png", "");

        assertEquals(InlineMediaUrls.DEFAULT_AVATAR_TEXTURE, InlineMediaCache.placeholderTextureFor(avatar, false));
        assertEquals(InlineMediaUrls.DEFAULT_AVATAR_TEXTURE, InlineMediaCache.placeholderTextureFor(avatar, true));
    }

    @Test
    void nonAvatarInlineMediaKeepsGenericPlaceholders() {
        RichLiveMessage.GiftIconSegment giftIcon = new RichLiveMessage.GiftIconSegment("rose", "https://cdn.example/rose.png", "");

        assertEquals(InlineMediaCache.LOADING_TEXTURE, InlineMediaCache.placeholderTextureFor(giftIcon, false));
        assertEquals(InlineMediaCache.ERROR_TEXTURE, InlineMediaCache.placeholderTextureFor(giftIcon, true));
    }

    @Test
    void cleanupEvictsOldestEligibleEntryWithoutTouchingRecentOrLoadingEntries() {
        InlineMediaCache cache = new InlineMediaCache();
        long now = System.currentTimeMillis();
        cache.debugPutReady("https://cdn.example/old.png", now - 1_000, 0L, 32, 16);
        cache.debugPutReady("https://cdn.example/recent.png", now - 900, now + 10_000, 32, 16);
        cache.debugPutLoading("https://cdn.example/loading.png", now - 5_000);
        for (int i = 0; i < 260; i++) {
            cache.debugPutReady("https://cdn.example/bulk-" + i + ".png", now - 800 + i, 0L, 16, 16);
        }

        cache.debugCleanup(now);

        assertFalse(cache.debugContains("https://cdn.example/old.png"));
        assertTrue(cache.debugContains("https://cdn.example/recent.png"));
        assertTrue(cache.debugContains("https://cdn.example/loading.png"));
        assertTrue(cache.snapshot().capacityEvictions() > 0);
    }

    @Test
    void ttlCleanupSkipsRecentlyVisibleEntries() {
        InlineMediaCache cache = new InlineMediaCache();
        long now = System.currentTimeMillis();
        cache.debugPutReady("https://cdn.example/expired.png", now - (31L * 60_000L), 0L, 16, 16);
        cache.debugPutReady("https://cdn.example/protected.png", now - (31L * 60_000L), now + 5_000L, 16, 16);

        cache.debugCleanup(now);

        assertFalse(cache.debugContains("https://cdn.example/expired.png"));
        assertTrue(cache.debugContains("https://cdn.example/protected.png"));
        assertEquals(1L, cache.snapshot().ttlEvictions());
    }

    @Test
    void visibleReadyAssetStaysReadyAfterCapacityCleanup() {
        InlineMediaCache cache = new InlineMediaCache();
        long now = System.currentTimeMillis();
        String protectedUrl = "https://cdn.example/protected-ready.png";
        RichLiveMessage.RemoteEmoteSegment protectedSegment = new RichLiveMessage.RemoteEmoteSegment("protected", protectedUrl, "[emote]");
        cache.debugPutReady(protectedUrl, now - 10_000L, 0L, 48, 24);
        cache.debugTouchVisible(protectedUrl, now);
        for (int i = 0; i < 300; i++) {
            cache.debugPutReady("https://cdn.example/other-" + i + ".png", now - 20_000L - i, 0L, 16, 16);
        }

        cache.debugCleanup(now);
        InlineMediaCache.TextureHandle handle = cache.resolve(protectedSegment);

        assertTrue(cache.debugContains(protectedUrl));
        assertTrue(cache.debugIsRecentlyVisible(protectedUrl, now));
        assertFalse(handle.loading());
        assertFalse(handle.error());
        assertEquals(48, handle.sourceWidth());
        assertEquals(24, handle.sourceHeight());
    }
}
