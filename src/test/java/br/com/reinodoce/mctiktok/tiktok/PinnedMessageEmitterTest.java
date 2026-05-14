package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.pinned.PinnedLiveMessage;
import br.com.reinodoce.mctiktok.pinned.PinnedMessageSink;
import br.com.reinodoce.mctiktok.rules.MessageRuleEngine;
import io.github.jwdeveloper.tiktok.messages.data.User;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastChatMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastRoomPinMessage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PinnedMessageEmitterTest {
    @Test
    void pinnedEventUsesOverlayAndTikTokDurationWithoutMirroringByDefault() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        RecordingPinnedSink pinnedSink = new RecordingPinnedSink();
        RecordingChatSink chatSink = new RecordingChatSink();
        PinnedMessageEmitter emitter = emitter(config, pinnedSink, chatSink);

        emitter.emit(roomPinMessage("hello pinned", 4L, 99L));

        assertEquals(1, pinnedSink.messages.size());
        assertEquals("hello pinned", pinnedSink.messages.get(0).message());
        assertEquals(Duration.ofSeconds(4), pinnedSink.messages.get(0).duration());
        assertEquals(99L, pinnedSink.messages.get(0).pinId());
        assertEquals(0, chatSink.pinnedMessages);
    }

    @Test
    void mirrorOutputCanBeEnabledIndependentlyFromOverlay() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setPinnedOverlayEnabled(false);
        config.setPinnedMessagesInOutput(true);
        config.setChatEmotesEnabled(false);
        RecordingPinnedSink pinnedSink = new RecordingPinnedSink();
        RecordingChatSink chatSink = new RecordingChatSink();
        PinnedMessageEmitter emitter = emitter(config, pinnedSink, chatSink);

        emitter.emit(roomPinMessage("mirror me", 3L, 100L));

        assertEquals(0, pinnedSink.messages.size());
        assertEquals(1, chatSink.pinnedMessages);
        assertEquals("mirror me", chatSink.lastPinnedText);
    }

    @Test
    void fallbackDurationIsUsedWhenTikTokDurationIsMissing() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        RecordingPinnedSink pinnedSink = new RecordingPinnedSink();
        PinnedMessageEmitter emitter = emitter(config, pinnedSink, new RecordingChatSink());

        emitter.emit(roomPinMessage("fallback", 0L, 101L));

        assertEquals(PinnedLiveMessage.DEFAULT_DURATION, pinnedSink.messages.get(0).duration());
    }

    @Test
    void inlineMediaOnlyPinnedEventUsesVisibleOverlayFallback() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        RecordingPinnedSink pinnedSink = new RecordingPinnedSink();
        PinnedMessageEmitter emitter = emitter(
                config,
                pinnedSink,
                new RecordingChatSink(),
                inlineMediaOnlyParser());

        emitter.emit(roomPinMessage("", 3L, 102L));

        assertEquals(1, pinnedSink.messages.size());
        assertEquals("[media]", pinnedSink.messages.get(0).message());
    }

    private static PinnedMessageEmitter emitter(
            ReinodoceConfig config,
            RecordingPinnedSink pinnedSink,
            RecordingChatSink chatSink
    ) {
        return emitter(config, pinnedSink, chatSink, new TikTokRichMessageParser());
    }

    private static PinnedMessageEmitter emitter(
            ReinodoceConfig config,
            RecordingPinnedSink pinnedSink,
            RecordingChatSink chatSink,
            TikTokRichMessageParser parser
    ) {
        return new PinnedMessageEmitter(new PinnedMessageEmitter.Dependencies(
                () -> config,
                chatSink,
                pinnedSink,
                new MessageRuleEngine(),
                new MemberLevelResolver(),
                parser,
                null));
    }

    private static TikTokRichMessageParser inlineMediaOnlyParser() {
        return new TikTokRichMessageParser() {
            @Override
            public RichLiveMessage parseChatMessage(WebcastChatMessage message, String username) {
                return new RichLiveMessage(
                        7L,
                        username,
                        List.of(new RichLiveMessage.AvatarSegment("https://cdn.example/avatar.png", "")));
            }
        };
    }

    private static WebcastRoomPinMessage roomPinMessage(String content, long displayDuration, long pinId) {
        User rawUser = User.newBuilder()
                .setId(123L)
                .setNickname("Alice")
                .setUsername("alice")
                .setFansClubInfo(User.FansClubInfo.newBuilder().setFansLevel(1))
                .build();
        WebcastChatMessage chatMessage = WebcastChatMessage.newBuilder()
                .setUser(rawUser)
                .setContent(content)
                .build();
        return WebcastRoomPinMessage.newBuilder()
                .setChatMessage(chatMessage)
                .setDisplayDuration(displayDuration)
                .setPinId(pinId)
                .build();
    }

    private static final class RecordingPinnedSink implements PinnedMessageSink {
        private final List<PinnedLiveMessage> messages = new ArrayList<>();

        @Override
        public void showPinnedMessage(ReinodoceConfig config, PinnedLiveMessage message) {
            messages.add(message);
        }
    }

    private static final class RecordingChatSink implements ChatEventSink {
        private int pinnedMessages;
        private String lastPinnedText = "";

        @Override
        public void sendLiveComment(ReinodoceConfig config, String username, String message) {
        }

        @Override
        public void sendLiveComment(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendStarComment(ReinodoceConfig config, String username, String message) {
        }

        @Override
        public void sendStarComment(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendPinnedComment(ReinodoceConfig config, String username, String message) {
            pinnedMessages++;
            lastPinnedText = message;
        }

        @Override
        public void sendPinnedComment(ReinodoceConfig config, RichLiveMessage message) {
            pinnedMessages++;
            lastPinnedText = message.plainText();
        }

        @Override
        public void sendSyntheticGift(ReinodoceConfig config, String username, String giftName, int count) {
        }

        @Override
        public void sendSyntheticGift(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendSyntheticFollow(ReinodoceConfig config, String username) {
        }

        @Override
        public void sendSyntheticFollow(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendSyntheticJoin(ReinodoceConfig config, String username) {
        }

        @Override
        public void sendSyntheticJoin(ReinodoceConfig config, RichLiveMessage message) {
        }

        @Override
        public void sendSyntheticMemberLevel(ReinodoceConfig config, String username, int memberLevel) {
        }

        @Override
        public void sendSyntheticMemberLevel(ReinodoceConfig config, RichLiveMessage message, int memberLevel) {
        }

        @Override
        public void sendSystem(String message, boolean success) {
        }
    }
}
