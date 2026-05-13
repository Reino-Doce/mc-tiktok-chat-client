package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class LiveMessageFormatterTest {
    private final InlineMediaTokenRegistry tokenRegistry = new InlineMediaTokenRegistry(new InlineMediaCache());
    private final LiveMessageFormatter formatter = new LiveMessageFormatter(tokenRegistry);
    private final ChatMessageStyle liveStyle = new ChatMessageStyle("[LIVE]", "{prefix}  <{username}> {message}");

    @Test
    void richMessagesInsertInlineTokens() {
        RichLiveMessage.AvatarSegment avatar = new RichLiveMessage.AvatarSegment("resource://reinodoce_mctiktok/textures/gui/no_user_image.png", "");
        RichLiveMessage.UnicodeEmojiSegment authorEmoji = new RichLiveMessage.UnicodeEmojiSegment("\uD83D\uDE00", "1f600", "\uD83D\uDE00");
        RichLiveMessage.RemoteEmoteSegment bodyEmote = new RichLiveMessage.RemoteEmoteSegment("wave", "https://cdn.example/wave.png", "[emote]");
        RichLiveMessage message = new RichLiveMessage(
                7L,
                List.of(
                        avatar,
                        new RichLiveMessage.TextSegment(" alice "),
                        authorEmoji
                ),
                List.of(
                        new RichLiveMessage.TextSegment("oi "),
                        bodyEmote,
                        new RichLiveMessage.TextSegment(" mundo")
                )
        );

        FormattedLiveComment formatted = formatter.formatLiveComment(liveStyle, message);
        String avatarToken = tokenRegistry.tokenFor(avatar);
        String authorToken = tokenRegistry.tokenFor(authorEmoji);
        String bodyToken = tokenRegistry.tokenFor(bodyEmote);

        assertEquals(
                "[LIVE]  <" + avatarToken + " alice " + authorToken + "> oi " + bodyToken + " mundo",
                formatted.component().getString()
        );
        assertEquals(message, formatted.richMessage());
        assertTrue(avatarToken.codePointAt(0) >= InlineMediaTokenRegistry.MIN_CODE_POINT);
        assertTrue(authorToken.codePointAt(0) >= InlineMediaTokenRegistry.MIN_CODE_POINT);
        assertTrue(bodyToken.codePointAt(0) >= InlineMediaTokenRegistry.MIN_CODE_POINT);
    }

    @Test
    void syntheticGiftIncludesGiftIconTokenBeforeGiftName() {
        RichLiveMessage.AvatarSegment avatar = new RichLiveMessage.AvatarSegment("resource://reinodoce_mctiktok/textures/gui/no_user_image.png", "");
        RichLiveMessage.GiftIconSegment giftIcon = new RichLiveMessage.GiftIconSegment("rose", "https://cdn.example/rose.png", "");
        RichLiveMessage message = new RichLiveMessage(
                9L,
                List.of(avatar, new RichLiveMessage.TextSegment(" alice")),
                List.of(
                        new RichLiveMessage.TextSegment("enviou "),
                        giftIcon,
                        new RichLiveMessage.TextSegment(" Rosa x3")
                )
        );

        FormattedLiveComment formatted = formatter.formatSyntheticGift(liveStyle, message);

        assertEquals(
                "[LIVE]  <" + tokenRegistry.tokenFor(avatar) + " alice> enviou " + tokenRegistry.tokenFor(giftIcon) + " Rosa x3",
                formatted.component().getString()
        );
    }

    @Test
    void plainRichMessagesStayAsPlainText() {
        RichLiveMessage message = new RichLiveMessage(
                8L,
                "alice",
                List.of(new RichLiveMessage.TextSegment("texto puro"))
        );

        FormattedLiveComment formatted = formatter.formatLiveComment(liveStyle, message);

        assertEquals("[LIVE]  <alice> texto puro", formatted.component().getString());
        assertEquals(message, formatted.richMessage());
    }

    @Test
    void starCommentsAddGoldMarkerBeforeAuthor() {
        RichLiveMessage message = new RichLiveMessage(
                11L,
                "alice",
                List.of(new RichLiveMessage.TextSegment("comentario destacado"))
        );

        FormattedLiveComment formatted = formatter.formatStarComment(liveStyle, message);

        assertEquals("[LIVE] \u2b50 STAR <alice> comentario destacado", formatted.component().getString());
        assertEquals(message, formatted.richMessage());
    }

    @Test
    void customTemplateCanReorderPlainMessages() {
        ChatMessageStyle style = new ChatMessageStyle("[TikTok]", "{username}: {message} {prefix}");

        assertEquals(
                "alice: oi [TikTok]",
                formatter.formatLiveComment(style, "alice", "oi").getString()
        );
    }

    @Test
    void customTemplatePreservesRichInlineTokens() {
        RichLiveMessage.AvatarSegment avatar = new RichLiveMessage.AvatarSegment(
                "resource://reinodoce_mctiktok/textures/gui/no_user_image.png", "");
        RichLiveMessage.RemoteEmoteSegment bodyEmote = new RichLiveMessage.RemoteEmoteSegment(
                "wave", "https://cdn.example/wave.png", "[emote]");
        RichLiveMessage message = new RichLiveMessage(
                12L,
                List.of(avatar, new RichLiveMessage.TextSegment("alice")),
                List.of(new RichLiveMessage.TextSegment("oi "), bodyEmote)
        );
        ChatMessageStyle style = new ChatMessageStyle("[TikTok]", "{prefix} {message} - {username}");

        FormattedLiveComment formatted = formatter.formatLiveComment(style, message);

        assertEquals(
                "[TikTok] oi " + tokenRegistry.tokenFor(bodyEmote) + " - " + tokenRegistry.tokenFor(avatar) + "alice",
                formatted.component().getString()
        );
    }
}
