package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.emoji.TwemojiUrlResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable TikTok LIVE message split into author and body segments.
 *
 * @param messageId TikTok message id
 * @param authorSegments display-author segments
 * @param bodySegments message body segments
 */
public record RichLiveMessage(long messageId, List<Segment> authorSegments, List<Segment> bodySegments) {
    /**
     * Normalizes null segment lists to immutable empty lists.
     *
     * @param messageId TikTok message id
     * @param authorSegments display-author segments
     * @param bodySegments message body segments
     */
    public RichLiveMessage {
        authorSegments = authorSegments == null ? List.of() : List.copyOf(authorSegments);
        bodySegments = bodySegments == null ? List.of() : List.copyOf(bodySegments);
    }

    /**
     * Creates a message from a plain username and body segment list.
     *
     * @param messageId TikTok message id
     * @param username plain display username
     * @param bodySegments message body segments
     */
    public RichLiveMessage(long messageId, String username, List<Segment> bodySegments) {
        this(messageId, List.of(new TextSegment(username)), bodySegments);
    }

    /**
     * Reports whether author or body segments include inline media.
     *
     * @return true when any segment is inline media
     */
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

    /**
     * Reports whether the message includes emote-like inline media.
     *
     * @return true when inline media is present
     */
    public boolean hasEmotes() {
        return hasInlineMedia();
    }

    /**
     * Flattens author segments into display text.
     *
     * @return plain author text
     */
    public String username() {
        StringBuilder builder = new StringBuilder();
        for (Segment segment : authorSegments) {
            builder.append(segment.plainText());
        }
        return builder.toString();
    }

    /**
     * Flattens body segments into display text.
     *
     * @return plain body text
     */
    public String plainText() {
        StringBuilder builder = new StringBuilder();
        for (Segment segment : bodySegments) {
            builder.append(segment.plainText());
        }
        return builder.toString();
    }

    /**
     * Flattens author and body segments into one text string.
     *
     * @return full plain text
     */
    public String fullPlainText() {
        return username() + plainText();
    }

    /**
     * Returns body segments for compatibility with older call sites.
     *
     * @return body segments
     */
    public List<Segment> segments() {
        return bodySegments;
    }

    /**
     * Returns all inline media segments from the author and body.
     *
     * @return inline media segments in render order
     */
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

    /**
     * Creates a copy with replacement author segments.
     *
     * @param authorSegments replacement author segments
     * @return copied message
     */
    public RichLiveMessage withAuthorSegments(List<Segment> authorSegments) {
        return new RichLiveMessage(messageId, authorSegments, bodySegments);
    }

    /**
     * Creates a copy with replacement body segments.
     *
     * @param bodySegments replacement body segments
     * @return copied message
     */
    public RichLiveMessage withBodySegments(List<Segment> bodySegments) {
        return new RichLiveMessage(messageId, authorSegments, bodySegments);
    }

    /**
     * Renderable message segment.
     */
    public sealed interface Segment permits TextSegment, InlineMediaSegment {
        /**
         * Returns fallback plain text for this segment.
         *
         * @return plain text
         */
        String plainText();
    }

    /**
     * Plain text segment.
     *
     * @param text plain text
     */
    public record TextSegment(String text) implements Segment {
        /**
         * Normalizes null text to blank text.
         *
         * @param text plain text
         */
        public TextSegment {
            text = text == null ? "" : text;
        }

        @Override
        public String plainText() {
            return text;
        }
    }

    /**
     * Segment backed by an image or texture that can be rendered inline in chat.
     */
    public sealed interface InlineMediaSegment extends Segment
            permits RemoteEmoteSegment, UnicodeEmojiSegment, AvatarSegment, GiftIconSegment {
        /**
         * Returns the broad media kind.
         *
         * @return media kind
         */
        InlineMediaKind kind();

        /**
         * Returns the render style.
         *
         * @return render style
         */
        InlineMediaRenderStyle renderStyle();

        /**
         * Returns a stable source key for cache identity.
         *
         * @return source key
         */
        String sourceKey();

        /**
         * Returns the source URL or resource URL.
         *
         * @return source URL
         */
        String sourceUrl();

        /**
         * Returns text to use when the media cannot render.
         *
         * @return fallback text
         */
        String fallbackText();

        @Override
        default String plainText() {
            return fallbackText();
        }
    }

    /**
     * Inline media source category.
     */
    public enum InlineMediaKind {
        /** Remote TikTok emote image. */
        REMOTE_EMOTE("remote_emote"),
        /** Twemoji-backed Unicode emoji image. */
        UNICODE_EMOJI("unicode_emoji"),
        /** User avatar image. */
        AVATAR("avatar"),
        /** Gift icon image. */
        GIFT_ICON("gift_icon");

        private final String identifier;

        InlineMediaKind(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Returns the stable identifier used in cache keys.
         *
         * @return media kind identifier
         */
        public String id() {
            return identifier;
        }
    }

    /**
     * Inline media layout policy.
     */
    public enum InlineMediaRenderStyle {
        /** Preserve the source aspect ratio. */
        SOURCE_ASPECT,
        /** Render in a square crop used for avatars and gift icons. */
        SQUARE_CROP
    }

    /**
     * Remote TikTok emote segment.
     *
     * @param emoteId TikTok emote id
     * @param imageUrl emote image URL
     * @param fallbackText fallback text
     */
    public record RemoteEmoteSegment(String emoteId, String imageUrl, String fallbackText) implements InlineMediaSegment {
        /**
         * Normalizes null values and supplies a default fallback label.
         *
         * @param emoteId TikTok emote id
         * @param imageUrl emote image URL
         * @param fallbackText fallback text
         */
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

    /**
     * Unicode emoji segment rendered through Twemoji assets.
     *
     * @param emojiText original emoji text
     * @param twemojiIconId Twemoji icon id
     * @param fallbackText fallback text
     */
    public record UnicodeEmojiSegment(String emojiText, String twemojiIconId, String fallbackText) implements InlineMediaSegment {
        /**
         * Normalizes null values and falls back to the emoji text.
         *
         * @param emojiText original emoji text
         * @param twemojiIconId Twemoji icon id
         * @param fallbackText fallback text
         */
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

    /**
     * User avatar segment rendered as a square crop.
     *
     * @param imageUrl avatar image URL
     * @param fallbackText fallback text
     */
    public record AvatarSegment(String imageUrl, String fallbackText) implements InlineMediaSegment {
        /**
         * Normalizes null avatar values.
         *
         * @param imageUrl avatar image URL
         * @param fallbackText fallback text
         */
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

    /**
     * Gift icon segment rendered as a square crop.
     *
     * @param giftId TikTok gift id
     * @param imageUrl gift image URL
     * @param fallbackText fallback text
     */
    public record GiftIconSegment(String giftId, String imageUrl, String fallbackText) implements InlineMediaSegment {
        /**
         * Normalizes null gift icon values.
         *
         * @param giftId TikTok gift id
         * @param imageUrl gift image URL
         * @param fallbackText fallback text
         */
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
