package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveMessageFormatterTest {
    private final InlineMediaTokenRegistry tokenRegistry = new InlineMediaTokenRegistry(new InlineMediaCache());
    private final LiveMessageFormatter formatter = new LiveMessageFormatter(tokenRegistry);

    @Test
    void richMessagesInsertInlineTokens() {
        RichLiveMessage.UnicodeEmojiSegment authorEmoji = new RichLiveMessage.UnicodeEmojiSegment("\uD83D\uDE00", "1f600", "\uD83D\uDE00");
        RichLiveMessage.RemoteEmoteSegment bodyEmote = new RichLiveMessage.RemoteEmoteSegment("wave", "https://cdn.example/wave.png", "[emote]");
        RichLiveMessage message = new RichLiveMessage(
                7L,
                List.of(
                        new RichLiveMessage.TextSegment("alice "),
                        authorEmoji
                ),
                List.of(
                        new RichLiveMessage.TextSegment("oi "),
                        bodyEmote,
                        new RichLiveMessage.TextSegment(" mundo")
                )
        );

        FormattedLiveComment formatted = formatter.formatLiveComment("[LIVE]", message);
        String authorToken = tokenRegistry.tokenFor(authorEmoji);
        String bodyToken = tokenRegistry.tokenFor(bodyEmote);

        assertEquals(
                "[LIVE]  <alice " + authorToken + "> oi " + bodyToken + " mundo",
                formatted.component().getString()
        );
        assertEquals(message, formatted.richMessage());
        assertTrue(authorToken.codePointAt(0) >= InlineMediaTokenRegistry.MIN_CODE_POINT);
        assertTrue(bodyToken.codePointAt(0) >= InlineMediaTokenRegistry.MIN_CODE_POINT);
    }

    @Test
    void plainRichMessagesStayAsPlainText() {
        RichLiveMessage message = new RichLiveMessage(
                8L,
                "alice",
                List.of(new RichLiveMessage.TextSegment("texto puro"))
        );

        FormattedLiveComment formatted = formatter.formatLiveComment("[LIVE]", message);

        assertEquals("[LIVE]  <alice> texto puro", formatted.component().getString());
        assertEquals(message, formatted.richMessage());
    }
}
