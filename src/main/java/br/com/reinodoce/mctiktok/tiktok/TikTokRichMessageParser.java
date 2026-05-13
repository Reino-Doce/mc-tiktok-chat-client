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

/**
 * Parses TikTok protobuf message text and emote structures into rich chat messages.
 */
public class TikTokRichMessageParser {
    /**
     * Parses a standard chat message.
     *
     * @param message TikTok chat message
     * @param username display username
     * @return rich chat message
     */
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

    /**
     * Parses display text from common message data.
     *
     * @param common common TikTok message data
     * @return parsed text
     */
    public ParsedText parseDisplayText(CommonMessageData common) {
        if (common == null || !common.hasDisplayText()) {
            return ParsedText.empty();
        }
        return parseText(common.getDisplayText());
    }

    /**
     * Parses TikTok text into rich segments.
     *
     * @param text TikTok text protobuf
     * @return parsed text
     */
    public ParsedText parseText(Text text) {
        return parseText(text, false);
    }

    /**
     * Parses barrage message content, suppressing duplicated leading user pieces.
     *
     * @param message TikTok barrage message
     * @return parsed text
     */
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

    /**
     * Parses an emote-only chat message.
     *
     * @param message TikTok emote chat message
     * @param username display username
     * @return rich chat message
     */
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

    /**
     * Parsed text plus any author hints discovered in TikTok text pieces.
     *
     * @param segments parsed rich segments
     * @param detectedUsername detected author username, or blank
     * @param detectedAvatarUrl detected author avatar URL, or blank
     * @param detectedUser detected TikTok protobuf user, or {@code null}
     */
    public record ParsedText(
            List<RichLiveMessage.Segment> segments,
            String detectedUsername,
            String detectedAvatarUrl,
            User detectedUser
    ) {
        /**
         * Normalizes null parsed fields.
         *
         * @param segments parsed rich segments
         * @param detectedUsername detected author username
         * @param detectedAvatarUrl detected author avatar URL
         * @param detectedUser detected TikTok user
         */
        public ParsedText {
            segments = segments == null ? List.of() : List.copyOf(segments);
            detectedUsername = detectedUsername == null ? "" : detectedUsername;
            detectedAvatarUrl = detectedAvatarUrl == null ? "" : detectedAvatarUrl;
        }

        /**
         * Returns an empty parsed-text value.
         *
         * @return empty parsed text
         */
        public static ParsedText empty() {
            return new ParsedText(List.of(), "", "", null);
        }

        /**
         * Reports whether any parsed segment should render visibly.
         *
         * @return true when the parsed text contains renderable content
         */
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

        /**
         * Flattens parsed segments into plain text.
         *
         * @return plain text
         */
        public String plainText() {
            StringBuilder builder = new StringBuilder();
            for (RichLiveMessage.Segment segment : segments) {
                builder.append(segment.plainText());
            }
            return builder.toString();
        }

        /**
         * Returns a copy with replacement detected-author fields.
         *
         * @param username detected username
         * @param avatarUrl detected avatar URL
         * @param user detected TikTok user
         * @return copied parsed text
         */
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
