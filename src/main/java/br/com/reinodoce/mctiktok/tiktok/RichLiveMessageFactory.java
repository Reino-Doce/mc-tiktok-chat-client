package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.emoji.UnicodeEmojiParser;
import br.com.reinodoce.mctiktok.i18n.Translations;

import java.util.ArrayList;
import java.util.List;

final class RichLiveMessageFactory {
    private static final String GIFT_PREFIX_KEY = "reinodoce.chat.gift_sent_prefix";
    private static final String GIFT_SUFFIX_KEY = "reinodoce.chat.gift_count_suffix";
    private static final String AUTHOR_SEPARATOR = " ";

    private final UnicodeEmojiParser unicodeEmojiParser;

    RichLiveMessageFactory(UnicodeEmojiParser unicodeEmojiParser) {
        this.unicodeEmojiParser = unicodeEmojiParser;
    }

    RichLiveMessage richTextMessage(long messageId, String username, String avatarUrl, String bodyText) {
        return new RichLiveMessage(
                messageId,
                authorSegments(username, avatarUrl),
                unicodeEmojiParser.parseText(bodyText));
    }

    RichLiveMessage richAuthorOnlyMessage(String username, String avatarUrl) {
        return new RichLiveMessage(0L, authorSegments(username, avatarUrl), List.of());
    }

    RichLiveMessage richGiftMessage(
            String username, String avatarUrl, String giftName, String giftIconUrl, int count
    ) {
        return new RichLiveMessage(
                0L,
                authorSegments(username, avatarUrl),
                giftSegments(giftName, giftIconUrl, count));
    }

    RichLiveMessage enrichCommentMessage(String username, String avatarUrl, RichLiveMessage richMessage) {
        return new RichLiveMessage(
                richMessage.messageId(),
                authorSegments(username, avatarUrl),
                unicodeEmojiParser.expandSegments(richMessage.bodySegments()));
    }

    private List<RichLiveMessage.Segment> authorSegments(String username, String avatarUrl) {
        List<RichLiveMessage.Segment> segments = new ArrayList<>();
        segments.add(new RichLiveMessage.AvatarSegment(
                avatarUrl == null || avatarUrl.isBlank() ? TikTokMediaResolver.defaultAvatarUrl() : avatarUrl,
                ""));
        segments.add(new RichLiveMessage.TextSegment(AUTHOR_SEPARATOR));
        segments.addAll(unicodeEmojiParser.parseText(username));
        return segments;
    }

    private List<RichLiveMessage.Segment> giftSegments(String giftName, String giftIconUrl, int count) {
        List<RichLiveMessage.Segment> segments = new ArrayList<>();
        segments.add(new RichLiveMessage.TextSegment(Translations.tr(GIFT_PREFIX_KEY)));
        if (giftIconUrl != null && !giftIconUrl.isBlank()) {
            segments.add(new RichLiveMessage.GiftIconSegment(giftName, giftIconUrl, ""));
            segments.add(new RichLiveMessage.TextSegment(AUTHOR_SEPARATOR));
        }
        segments.addAll(unicodeEmojiParser.parseText(giftName));
        segments.add(new RichLiveMessage.TextSegment(
                Translations.tr(GIFT_SUFFIX_KEY, Math.max(1, count))));
        return segments;
    }
}
