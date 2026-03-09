package br.com.reinodoce.mctiktok.client.overlay;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
