package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import io.github.jwdeveloper.tiktok.messages.data.CommonMessageData;
import io.github.jwdeveloper.tiktok.messages.data.Emote;
import io.github.jwdeveloper.tiktok.messages.data.Image;
import io.github.jwdeveloper.tiktok.messages.data.Text;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TikTokRichMessageParserTest {
    private final TikTokRichMessageParser parser = new TikTokRichMessageParser();

    @Test
    void parsesDisplayTextIntoOrderedSegments() {
        WebcastChatMessage message = WebcastChatMessage.newBuilder()
                .setCommon(CommonMessageData.newBuilder()
                        .setMsgId(42L)
                        .setDisplayText(Text.newBuilder()
                                .addPiecesList(Text.TextPiece.newBuilder().setStringValue("oi "))
                                .addPiecesList(Text.TextPiece.newBuilder()
                                        .setImageValue(Text.TextPieceImage.newBuilder()
                                                .setImageModel(Image.newBuilder().addUrl("https://cdn.example/emote-sign-.png"))))
                                .addPiecesList(Text.TextPiece.newBuilder().setStringValue("mundo"))
                                .build())
                        .build())
                .setContent("oi mundo")
                .build();

        RichLiveMessage richMessage = parser.parseChatMessage(message, "alice");

        assertEquals(42L, richMessage.messageId());
        assertTrue(richMessage.hasEmotes());
        assertEquals(3, richMessage.segments().size());
        assertEquals("oi ", ((RichLiveMessage.TextSegment) richMessage.segments().get(0)).text());
        RichLiveMessage.RemoteEmoteSegment emote = assertInstanceOf(RichLiveMessage.RemoteEmoteSegment.class, richMessage.segments().get(1));
        assertEquals("https://cdn.example/emote-.png", emote.imageUrl());
        assertEquals("mundo", ((RichLiveMessage.TextSegment) richMessage.segments().get(2)).text());
    }

    @Test
    void fallsBackToContentAndEmoteIndicesWhenDisplayTextIsMissing() {
        WebcastChatMessage message = WebcastChatMessage.newBuilder()
                .setCommon(CommonMessageData.newBuilder().setMsgId(99L).build())
                .setContent("oi \uE123mundo")
                .addEmotesList(WebcastChatMessage.EmoteWithIndex.newBuilder()
                        .setIndex(3)
                        .setEmote(Emote.newBuilder()
                                .setEmoteId("wave")
                                .setImage(Image.newBuilder().addUrl("https://cdn.example/wave.png"))))
                .build();

        RichLiveMessage richMessage = parser.parseChatMessage(message, "alice");

        assertEquals(3, richMessage.segments().size());
        assertEquals("oi ", ((RichLiveMessage.TextSegment) richMessage.segments().get(0)).text());
        assertInstanceOf(RichLiveMessage.RemoteEmoteSegment.class, richMessage.segments().get(1));
        assertEquals("mundo", ((RichLiveMessage.TextSegment) richMessage.segments().get(2)).text());
        assertEquals("oi [emote]mundo", richMessage.plainText());
    }

    @Test
    void prefersIndexedFallbackWhenDisplayTextHasNoImageSegments() {
        WebcastChatMessage message = WebcastChatMessage.newBuilder()
                .setCommon(CommonMessageData.newBuilder()
                        .setMsgId(100L)
                        .setDisplayText(Text.newBuilder()
                                .addPiecesList(Text.TextPiece.newBuilder().setStringValue("oi mundo"))
                                .build())
                        .build())
                .setContent("oi \uE321mundo")
                .addEmotesList(WebcastChatMessage.EmoteWithIndex.newBuilder()
                        .setIndex(3)
                        .setEmote(Emote.newBuilder()
                                .setEmoteId("wave")
                                .setImage(Image.newBuilder().addUrl("https://cdn.example/wave.png"))))
                .build();

        RichLiveMessage richMessage = parser.parseChatMessage(message, "alice");

        assertEquals(3, richMessage.segments().size());
        assertEquals("oi ", ((RichLiveMessage.TextSegment) richMessage.segments().get(0)).text());
        assertInstanceOf(RichLiveMessage.RemoteEmoteSegment.class, richMessage.segments().get(1));
        assertEquals("mundo", ((RichLiveMessage.TextSegment) richMessage.segments().get(2)).text());
    }

    @Test
    void preservesRegularEmojiBeforeIndexedCustomEmote() {
        WebcastChatMessage message = WebcastChatMessage.newBuilder()
                .setCommon(CommonMessageData.newBuilder().setMsgId(101L).build())
                .setContent("hi \uD83D\uDE00\uE111 there")
                .addEmotesList(WebcastChatMessage.EmoteWithIndex.newBuilder()
                        .setIndex(4)
                        .setEmote(Emote.newBuilder()
                                .setEmoteId("mvp")
                                .setImage(Image.newBuilder().addUrl("https://cdn.example/mvp.png"))))
                .build();

        RichLiveMessage richMessage = parser.parseChatMessage(message, "alice");

        assertEquals(3, richMessage.segments().size());
        assertEquals("hi \uD83D\uDE00", ((RichLiveMessage.TextSegment) richMessage.segments().get(0)).text());
        assertInstanceOf(RichLiveMessage.RemoteEmoteSegment.class, richMessage.segments().get(1));
        assertEquals(" there", ((RichLiveMessage.TextSegment) richMessage.segments().get(2)).text());
    }
}
