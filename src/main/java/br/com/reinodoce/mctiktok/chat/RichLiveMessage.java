package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.emoji.TwemojiUrlResolver;

import java.util.ArrayList;
import java.util.List;

public record RichLiveMessage(long messageId, List<Segment> authorSegments, List<Segment> bodySegments) {
    public RichLiveMessage {
        authorSegments = authorSegments == null ? List.of() : List.copyOf(authorSegments);
        bodySegments = bodySegments == null ? List.of() : List.copyOf(bodySegments);
    }

    public RichLiveMessage(long messageId, String username, List<Segment> bodySegments) {
        this(messageId, List.of(new TextSegment(username)), bodySegments);
    }

    public boolean hasInlineMedia() {
        for (Segment segment : authorSegments) {
            if (segment instanceof InlineMediaSegment) {
                return true;
            }
        }
        for (Segment segment : bodySegments) {
            if (segment instanceof InlineMediaSegment) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEmotes() {
        return hasInlineMedia();
    }

    public String username() {
        StringBuilder builder = new StringBuilder();
        for (Segment segment : authorSegments) {
            builder.append(segment.plainText());
        }
        return builder.toString();
    }

    public String plainText() {
        StringBuilder builder = new StringBuilder();
        for (Segment segment : bodySegments) {
            builder.append(segment.plainText());
        }
        return builder.toString();
    }

    public String fullPlainText() {
        return username() + plainText();
    }

    public List<Segment> segments() {
        return bodySegments;
    }

    public List<InlineMediaSegment> inlineMediaSegments() {
        List<InlineMediaSegment> media = new ArrayList<>();
        for (Segment segment : authorSegments) {
            if (segment instanceof InlineMediaSegment inlineMediaSegment) {
                media.add(inlineMediaSegment);
            }
        }
        for (Segment segment : bodySegments) {
            if (segment instanceof InlineMediaSegment inlineMediaSegment) {
                media.add(inlineMediaSegment);
            }
        }
        return media;
    }

    public RichLiveMessage withAuthorSegments(List<Segment> authorSegments) {
        return new RichLiveMessage(messageId, authorSegments, bodySegments);
    }

    public RichLiveMessage withBodySegments(List<Segment> bodySegments) {
        return new RichLiveMessage(messageId, authorSegments, bodySegments);
    }

    public sealed interface Segment permits TextSegment, InlineMediaSegment {
        String plainText();
    }

    public record TextSegment(String text) implements Segment {
        public TextSegment {
            text = text == null ? "" : text;
        }

        @Override
        public String plainText() {
            return text;
        }
    }

    public sealed interface InlineMediaSegment extends Segment
            permits RemoteEmoteSegment, UnicodeEmojiSegment, AvatarSegment, GiftIconSegment {
        InlineMediaKind kind();

        InlineMediaRenderStyle renderStyle();

        String sourceKey();

        String sourceUrl();

        String fallbackText();

        @Override
        default String plainText() {
            return fallbackText();
        }
    }

    public enum InlineMediaKind {
        REMOTE_EMOTE("remote_emote"),
        UNICODE_EMOJI("unicode_emoji"),
        AVATAR("avatar"),
        GIFT_ICON("gift_icon");

        private final String identifier;

        InlineMediaKind(String identifier) {
            this.identifier = identifier;
        }

        public String id() {
            return identifier;
        }
    }

    public enum InlineMediaRenderStyle {
        SOURCE_ASPECT,
        SQUARE_CROP
    }

    public record RemoteEmoteSegment(String emoteId, String imageUrl, String fallbackText) implements InlineMediaSegment {
        public RemoteEmoteSegment {
            emoteId = emoteId == null ? "" : emoteId;
            imageUrl = imageUrl == null ? "" : imageUrl;
            fallbackText = fallbackText == null || fallbackText.isBlank() ? "[emote]" : fallbackText;
        }

        @Override
        public InlineMediaKind kind() {
            return InlineMediaKind.REMOTE_EMOTE;
        }

        @Override
        public InlineMediaRenderStyle renderStyle() {
            return InlineMediaRenderStyle.SOURCE_ASPECT;
        }

        @Override
        public String sourceKey() {
            return emoteId.isBlank() ? imageUrl : emoteId;
        }

        @Override
        public String sourceUrl() {
            return imageUrl;
        }
    }

    public record UnicodeEmojiSegment(String emojiText, String twemojiIconId, String fallbackText) implements InlineMediaSegment {
        public UnicodeEmojiSegment {
            emojiText = emojiText == null ? "" : emojiText;
            twemojiIconId = twemojiIconId == null ? "" : twemojiIconId;
            fallbackText = fallbackText == null || fallbackText.isBlank() ? emojiText : fallbackText;
        }

        @Override
        public InlineMediaKind kind() {
            return InlineMediaKind.UNICODE_EMOJI;
        }

        @Override
        public InlineMediaRenderStyle renderStyle() {
            return InlineMediaRenderStyle.SOURCE_ASPECT;
        }

        @Override
        public String sourceKey() {
            return twemojiIconId;
        }

        @Override
        public String sourceUrl() {
            return TwemojiUrlResolver.toUrl(twemojiIconId);
        }
    }

    public record AvatarSegment(String imageUrl, String fallbackText) implements InlineMediaSegment {
        public AvatarSegment {
            imageUrl = imageUrl == null ? "" : imageUrl;
            fallbackText = fallbackText == null ? "" : fallbackText;
        }

        @Override
        public InlineMediaKind kind() {
            return InlineMediaKind.AVATAR;
        }

        @Override
        public InlineMediaRenderStyle renderStyle() {
            return InlineMediaRenderStyle.SQUARE_CROP;
        }

        @Override
        public String sourceKey() {
            return imageUrl;
        }

        @Override
        public String sourceUrl() {
            return imageUrl;
        }
    }

    public record GiftIconSegment(String giftId, String imageUrl, String fallbackText) implements InlineMediaSegment {
        public GiftIconSegment {
            giftId = giftId == null ? "" : giftId;
            imageUrl = imageUrl == null ? "" : imageUrl;
            fallbackText = fallbackText == null ? "" : fallbackText;
        }

        @Override
        public InlineMediaKind kind() {
            return InlineMediaKind.GIFT_ICON;
        }

        @Override
        public InlineMediaRenderStyle renderStyle() {
            return InlineMediaRenderStyle.SQUARE_CROP;
        }

        @Override
        public String sourceKey() {
            return giftId.isBlank() ? imageUrl : giftId;
        }

        @Override
        public String sourceUrl() {
            return imageUrl;
        }
    }
}
