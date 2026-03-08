package br.com.reinodoce.mctiktok.client.font;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlineMediaTokenRegistryTest {
    private final InlineMediaTokenRegistry registry = new InlineMediaTokenRegistry(new InlineMediaCache());

    @Test
    void deduplicatesTokensForSameInlineMediaAsset() {
        RichLiveMessage.UnicodeEmojiSegment first = new RichLiveMessage.UnicodeEmojiSegment("😀", "1f600", "😀");
        RichLiveMessage.UnicodeEmojiSegment second = new RichLiveMessage.UnicodeEmojiSegment("😀", "1f600", "😀");

        String firstToken = registry.tokenFor(first);
        String secondToken = registry.tokenFor(second);

        assertEquals(firstToken, secondToken);
        assertEquals(1, registry.size());
        assertTrue(firstToken.codePointAt(0) >= InlineMediaTokenRegistry.MIN_CODE_POINT);
    }

    @Test
    void lookupReturnsRegisteredEntryForSupplementaryToken() {
        RichLiveMessage.RemoteEmoteSegment emote = new RichLiveMessage.RemoteEmoteSegment("wave", "https://cdn.example/wave.png", "[emote]");

        String token = registry.tokenFor(emote);
        InlineMediaTokenRegistry.TokenEntry entry = registry.lookup(token.codePointAt(0));

        assertNotNull(entry);
        assertEquals(emote.sourceUrl(), entry.segment().sourceUrl());
        assertEquals(token, entry.token());
    }

    @Test
    void squareMediaUsesFixedChatHeightAdvance() {
        RichLiveMessage.AvatarSegment avatar = new RichLiveMessage.AvatarSegment("resource://reinodoce_mctiktok/textures/gui/no_user_image.png", "");
        RichLiveMessage.GiftIconSegment giftIcon = new RichLiveMessage.GiftIconSegment("rose", "https://cdn.example/rose.png", "");

        assertEquals(9.0F, registry.advanceFor(avatar));
        assertEquals(9.0F, registry.advanceFor(giftIcon));
    }
}
