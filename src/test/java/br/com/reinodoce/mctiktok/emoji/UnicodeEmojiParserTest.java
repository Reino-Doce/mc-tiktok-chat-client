package br.com.reinodoce.mctiktok.emoji;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnicodeEmojiParserTest {
    private final UnicodeEmojiParser parser = new UnicodeEmojiParser();

    @Test
    void parsesEmojiInsideDisplayNameText() {
        List<RichLiveMessage.Segment> segments = parser.parseText("alice 😀");

        assertEquals(2, segments.size());
        assertEquals("alice ", ((RichLiveMessage.TextSegment) segments.get(0)).text());
        assertEquals("1f600", assertInstanceOf(RichLiveMessage.UnicodeEmojiSegment.class, segments.get(1)).twemojiIconId());
    }

    @Test
    void replacesSimpleEmojiWithUnicodeEmojiSegment() {
        RichLiveMessage message = new RichLiveMessage(1L, "alice", List.of(new RichLiveMessage.TextSegment("oi 😀 mundo")));

        RichLiveMessage expanded = parser.expand(message);

        assertEquals(3, expanded.segments().size());
        assertEquals("oi ", ((RichLiveMessage.TextSegment) expanded.segments().get(0)).text());
        RichLiveMessage.UnicodeEmojiSegment emoji = assertInstanceOf(RichLiveMessage.UnicodeEmojiSegment.class, expanded.segments().get(1));
        assertEquals("😀", emoji.emojiText());
        assertEquals("1f600", emoji.twemojiIconId());
        assertEquals(" mundo", ((RichLiveMessage.TextSegment) expanded.segments().get(2)).text());
    }

    @Test
    void replacesFlagAndZwjSequencesUsingLongestMatch() {
        RichLiveMessage message = new RichLiveMessage(
                2L,
                "alice",
                List.of(new RichLiveMessage.TextSegment("🇧🇷 e 👩‍💻"))
        );

        RichLiveMessage expanded = parser.expand(message);

        assertEquals(3, expanded.segments().size());
        assertEquals("1f1e7-1f1f7", assertInstanceOf(RichLiveMessage.UnicodeEmojiSegment.class, expanded.segments().get(0)).twemojiIconId());
        assertEquals(" e ", ((RichLiveMessage.TextSegment) expanded.segments().get(1)).text());
        assertEquals("1f469-200d-1f4bb", assertInstanceOf(RichLiveMessage.UnicodeEmojiSegment.class, expanded.segments().get(2)).twemojiIconId());
        assertTrue(expanded.hasInlineMedia());
    }

    @Test
    void preservesNonEmojiTextSegments() {
        RichLiveMessage message = new RichLiveMessage(3L, "alice", List.of(new RichLiveMessage.TextSegment("sem emoji")));

        RichLiveMessage expanded = parser.expand(message);

        assertEquals(message.segments(), expanded.segments());
        assertEquals(message.authorSegments(), expanded.authorSegments());
    }
}
