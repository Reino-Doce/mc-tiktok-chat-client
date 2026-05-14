package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.emoji.UnicodeEmojiParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RichLiveMessageFactoryTest {
    @Test
    void richGiftMessageUsesConfiguredLanguageForFixedPhrases() {
        RichLiveMessageFactory factory = new RichLiveMessageFactory(new UnicodeEmojiParser(), () -> "pt_br");

        RichLiveMessage message = factory.richGiftMessage("alice", "", "Rosa", "", 3);

        assertEquals("enviou Rosa x3", message.plainText());
    }

    @Test
    void richGiftMessageFallsBackToEffectiveLanguageWhenRuntimeLanguageIsUnavailable() {
        RichLiveMessageFactory factory = new RichLiveMessageFactory(
                new UnicodeEmojiParser(), () -> "pt_br", () -> true);

        RichLiveMessage message = factory.richGiftMessage("alice", "", "Rosa", "", 3);

        assertEquals("enviou Rosa x3", message.plainText());
    }
}
