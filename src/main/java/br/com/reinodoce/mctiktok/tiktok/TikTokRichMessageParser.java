package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import io.github.jwdeveloper.tiktok.messages.data.CommonMessageData;
import io.github.jwdeveloper.tiktok.messages.data.Emote;
import io.github.jwdeveloper.tiktok.messages.data.Text;
import io.github.jwdeveloper.tiktok.messages.data.User;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastBarrageMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastChatMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastEmoteChatMessage;

import java.util.ArrayList;
import java.util.List;

public class TikTokRichMessageParser {
    public RichLiveMessage parseChatMessage(WebcastChatMessage message, String username) {
        if (message == null) {
            return new RichLiveMessage(0L, username, List.of());
        }
        List<RichLiveMessage.Segment> primary = parseDisplayText(messageCommon(message)).segments();
        List<RichLiveMessage.Segment> segments = applyIndexedEmoteFallback(message, primary);
        if (segments.isEmpty()) {
            segments = fallbackContentSegments(message.getContent());
        }
        return new RichLiveMessage(resolveMessageId(messageCommon(message)), username, segments);
    }

    public ParsedText parseDisplayText(CommonMessageData common) {
        if (common == null || !common.hasDisplayText()) {
            return ParsedText.empty();
        }
        return parseText(common.getDisplayText());
    }

    public ParsedText parseText(Text text) {
        return parseText(text, false);
    }

    public ParsedText parseBarrageText(WebcastBarrageMessage message) {
        if (message == null) {
            return ParsedText.empty();
        }
        ParsedText content = message.hasContent()
                ? parseText(message.getContent(), true)
                : ParsedText.empty();
        ParsedText common = message.hasCommonBarrageContent()
                ? parseText(message.getCommonBarrageContent(), true)
                : ParsedText.empty();
        return trimLeadingWhitespace(mergeBarrageParts(content, common));
    }

    public RichLiveMessage parseEmoteChatMessage(WebcastEmoteChatMessage message, String username) {
        if (message == null) {
            return new RichLiveMessage(0L, username, List.of());
        }
        SegmentBuilder buffer = new SegmentBuilder();
        for (Emote emote : message.getEmoteListList()) {
            buffer.appendEmote(emote);
        }
        return new RichLiveMessage(resolveMessageId(messageCommon(message)), username, buffer.build());
    }

    private ParsedText parseText(Text text, boolean suppressLeadingUserPiece) {
        if (text == null) {
            return ParsedText.empty();
        }
        SegmentBuilder buffer = new SegmentBuilder();
        TextPieceConsumer.DetectedAuthor author =
                TextPieceConsumer.consumeAll(text, buffer, suppressLeadingUserPiece);
        return new ParsedText(buffer.build(), author.username(), author.avatarUrl(), author.user());
    }

    private ParsedText mergeBarrageParts(ParsedText content, ParsedText common) {
        if (!content.hasRenderableContent()) {
            return common;
        }
        if (!common.hasRenderableContent()) {
            return content;
        }
        if (content.detectedUsername().isBlank() && !common.detectedUsername().isBlank()) {
            return content.withDetectedAuthor(
                    common.detectedUsername(), common.detectedAvatarUrl(), common.detectedUser());
        }
        return content;
    }

    private List<RichLiveMessage.Segment> applyIndexedEmoteFallback(
            WebcastChatMessage message, List<RichLiveMessage.Segment> primary
    ) {
        boolean hasIndexedEmotes = message.getEmotesListCount() > 0;
        if (hasIndexedEmotes && !containsEmoteSegment(primary)) {
            List<RichLiveMessage.Segment> indexed = mergeIndexedEmotes(message);
            return containsEmoteSegment(indexed) || primary.isEmpty() ? indexed : primary;
        }
        if (primary.isEmpty()) {
            return mergeIndexedEmotes(message);
        }
        return primary;
    }

    private List<RichLiveMessage.Segment> mergeIndexedEmotes(WebcastChatMessage message) {
        return EmoteIndexMerger.merge(
                message.getContent(), message.getEmotesListList(), new SegmentBuilder());
    }

    private List<RichLiveMessage.Segment> fallbackContentSegments(String content) {
        SegmentBuilder buffer = new SegmentBuilder();
        buffer.appendText(content);
        return buffer.build();
    }

    private boolean containsEmoteSegment(List<RichLiveMessage.Segment> segments) {
        for (RichLiveMessage.Segment segment : segments) {
            if (segment instanceof RichLiveMessage.RemoteEmoteSegment) {
                return true;
            }
        }
        return false;
    }

    private ParsedText trimLeadingWhitespace(ParsedText parsedText) {
        if (parsedText.segments().isEmpty()) {
            return parsedText;
        }
        if (!(parsedText.segments().get(0) instanceof RichLiveMessage.TextSegment firstSegment)) {
            return parsedText;
        }
        String trimmed = firstSegment.text().replaceFirst("^\\s+", "");
        if (trimmed.equals(firstSegment.text())) {
            return parsedText;
        }
        List<RichLiveMessage.Segment> segments = new ArrayList<>(parsedText.segments());
        if (trimmed.isBlank()) {
            segments.remove(0);
        } else {
            segments.set(0, new RichLiveMessage.TextSegment(trimmed));
        }
        return new ParsedText(
                segments,
                parsedText.detectedUsername(),
                parsedText.detectedAvatarUrl(),
                parsedText.detectedUser());
    }

    private static CommonMessageData messageCommon(WebcastChatMessage message) {
        return message.hasCommon() ? message.getCommon() : null;
    }

    private static CommonMessageData messageCommon(WebcastEmoteChatMessage message) {
        return message.hasCommon() ? message.getCommon() : null;
    }

    private static long resolveMessageId(CommonMessageData common) {
        return common == null ? 0L : common.getMsgId();
    }

    public record ParsedText(
            List<RichLiveMessage.Segment> segments,
            String detectedUsername,
            String detectedAvatarUrl,
            User detectedUser
    ) {
        public ParsedText {
            segments = segments == null ? List.of() : List.copyOf(segments);
            detectedUsername = detectedUsername == null ? "" : detectedUsername;
            detectedAvatarUrl = detectedAvatarUrl == null ? "" : detectedAvatarUrl;
        }

        public static ParsedText empty() {
            return new ParsedText(List.of(), "", "", null);
        }

        public boolean hasRenderableContent() {
            if (segments.isEmpty()) {
                return false;
            }
            for (RichLiveMessage.Segment segment : segments) {
                if (segmentIsRenderable(segment)) {
                    return true;
                }
            }
            return false;
        }

        public String plainText() {
            StringBuilder builder = new StringBuilder();
            for (RichLiveMessage.Segment segment : segments) {
                builder.append(segment.plainText());
            }
            return builder.toString();
        }

        public ParsedText withDetectedAuthor(String username, String avatarUrl, User user) {
            return new ParsedText(segments, username, avatarUrl, user);
        }

        private static boolean segmentIsRenderable(RichLiveMessage.Segment segment) {
            return segment instanceof RichLiveMessage.InlineMediaSegment
                    || (segment instanceof RichLiveMessage.TextSegment textSegment
                    && !textSegment.text().isBlank());
        }
    }
}
