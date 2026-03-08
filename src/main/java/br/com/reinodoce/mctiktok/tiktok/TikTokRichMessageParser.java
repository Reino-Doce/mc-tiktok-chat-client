package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import io.github.jwdeveloper.tiktok.messages.data.CommonMessageData;
import io.github.jwdeveloper.tiktok.messages.data.Emote;
import io.github.jwdeveloper.tiktok.messages.data.Image;
import io.github.jwdeveloper.tiktok.messages.data.Text;
import io.github.jwdeveloper.tiktok.messages.data.User;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastChatMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastEmoteChatMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TikTokRichMessageParser {
    public RichLiveMessage parseChatMessage(WebcastChatMessage message, String username) {
        if (message == null) {
            return new RichLiveMessage(0L, username, List.of());
        }

        List<RichLiveMessage.Segment> segments = parseDisplayText(message.hasCommon() ? message.getCommon() : null);
        boolean hasIndexedEmotes = message.getEmotesListCount() > 0;
        if (hasIndexedEmotes && !containsEmoteSegment(segments)) {
            List<RichLiveMessage.Segment> indexedSegments = parseContentAndEmotes(message.getContent(), message.getEmotesListList());
            if (containsEmoteSegment(indexedSegments) || segments.isEmpty()) {
                segments = indexedSegments;
            }
        } else if (segments.isEmpty()) {
            segments = parseContentAndEmotes(message.getContent(), message.getEmotesListList());
        }

        if (segments.isEmpty()) {
            List<RichLiveMessage.Segment> fallback = new ArrayList<>();
            appendText(fallback, message.getContent());
            segments = fallback;
        }

        return new RichLiveMessage(resolveMessageId(message.hasCommon() ? message.getCommon() : null), username, segments);
    }

    public RichLiveMessage parseEmoteChatMessage(WebcastEmoteChatMessage message, String username) {
        if (message == null) {
            return new RichLiveMessage(0L, username, List.of());
        }

        List<RichLiveMessage.Segment> segments = new ArrayList<>();
        for (Emote emote : message.getEmoteListList()) {
            appendEmote(segments, emote);
        }
        return new RichLiveMessage(resolveMessageId(message.hasCommon() ? message.getCommon() : null), username, segments);
    }

    private List<RichLiveMessage.Segment> parseDisplayText(CommonMessageData common) {
        if (common == null || !common.hasDisplayText()) {
            return List.of();
        }

        Text displayText = common.getDisplayText();
        List<RichLiveMessage.Segment> segments = new ArrayList<>();
        for (Text.TextPiece piece : displayText.getPiecesListList()) {
            if (piece.hasImageValue() && piece.getImageValue().hasImageModel()) {
                appendEmote(segments, piece.getImageValue().getImageModel());
                continue;
            }
            if (!piece.getStringValue().isBlank()) {
                appendText(segments, piece.getStringValue());
                continue;
            }
            if (piece.hasUserValue()) {
                appendText(segments, resolveDisplayName(piece.getUserValue().getUser()));
                continue;
            }
            if (piece.hasPatternRefValue()) {
                appendText(segments, piece.getPatternRefValue().getDefaultPattern());
                continue;
            }
            if (piece.hasHeartValue()) {
                appendText(segments, "\u2764");
                continue;
            }
            if (piece.hasGiftValue()) {
                appendText(segments, "[gift]");
            }
        }
        return segments;
    }

    private List<RichLiveMessage.Segment> parseContentAndEmotes(
            String rawContent,
            List<WebcastChatMessage.EmoteWithIndex> emotesWithIndex
    ) {
        String content = rawContent == null ? "" : rawContent;
        if ((content.isBlank()) && (emotesWithIndex == null || emotesWithIndex.isEmpty())) {
            return List.of();
        }

        int[] codePoints = content.codePoints().toArray();
        List<RichLiveMessage.Segment> segments = new ArrayList<>();
        if (emotesWithIndex == null || emotesWithIndex.isEmpty()) {
            appendText(segments, content);
            return segments;
        }

        List<WebcastChatMessage.EmoteWithIndex> sorted = new ArrayList<>(emotesWithIndex);
        sorted.sort(Comparator.comparingLong(WebcastChatMessage.EmoteWithIndex::getIndex));

        int cursor = 0;
        for (WebcastChatMessage.EmoteWithIndex emoteWithIndex : sorted) {
            int index = (int) Math.max(0, Math.min(codePoints.length, emoteWithIndex.getIndex()));
            if (index > cursor) {
                appendText(segments, codePointsToString(codePoints, cursor, index));
            }
            appendEmote(segments, emoteWithIndex.getEmote());
            cursor = skipPlaceholderCodePoints(codePoints, index);
        }
        if (cursor < codePoints.length) {
            appendText(segments, codePointsToString(codePoints, cursor, codePoints.length));
        }

        return segments;
    }

    private void appendText(List<RichLiveMessage.Segment> segments, String rawText) {
        String sanitized = sanitizeSegmentText(rawText);
        if (sanitized.isBlank()) {
            return;
        }

        if (!segments.isEmpty() && segments.get(segments.size() - 1) instanceof RichLiveMessage.TextSegment existing) {
            segments.set(segments.size() - 1, new RichLiveMessage.TextSegment(existing.text() + sanitized));
            return;
        }
        segments.add(new RichLiveMessage.TextSegment(sanitized));
    }

    private void appendEmote(List<RichLiveMessage.Segment> segments, Emote emote) {
        if (emote == null || !emote.hasImage()) {
            return;
        }
        appendEmote(segments, emote.getEmoteId(), emote.getImage());
    }

    private void appendEmote(List<RichLiveMessage.Segment> segments, Image image) {
        appendEmote(segments, "", image);
    }

    private void appendEmote(List<RichLiveMessage.Segment> segments, String emoteId, Image image) {
        String url = resolveImageUrl(image);
        if (url.isBlank()) {
            return;
        }
        segments.add(new RichLiveMessage.RemoteEmoteSegment(emoteId, url, "[emote]"));
    }

    private long resolveMessageId(CommonMessageData common) {
        return common == null ? 0L : common.getMsgId();
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "";
        }
        if (!MessageSanitizer.sanitize(user.getNickname()).isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private String resolveImageUrl(Image image) {
        if (image == null || image.getUrlCount() <= 0) {
            return "";
        }

        String raw = image.getUrl(image.getUrlCount() - 1);
        String sanitized = MessageSanitizer.sanitize(raw);
        if (sanitized.isBlank()) {
            return "";
        }
        return sanitized
                .replace("-sign-", "-")
                .replace("-sign.", ".");
    }

    private String sanitizeSegmentText(String rawText) {
        if (rawText == null) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(rawText.length());
        rawText.codePoints().forEach(codePoint -> {
            if (codePoint == 0) {
                return;
            }
            if (isPlaceholderCodePoint(codePoint)) {
                return;
            }
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return;
            }
            sanitized.appendCodePoint(codePoint);
        });

        String collapsedWhitespace = sanitized.toString().replaceAll("\\s+", " ");
        return collapsedWhitespace.length() > 256 ? collapsedWhitespace.substring(0, 256) : collapsedWhitespace;
    }

    private boolean containsEmoteSegment(List<RichLiveMessage.Segment> segments) {
        for (RichLiveMessage.Segment segment : segments) {
            if (segment instanceof RichLiveMessage.RemoteEmoteSegment) {
                return true;
            }
        }
        return false;
    }

    private int skipPlaceholderCodePoints(int[] codePoints, int index) {
        if (index >= codePoints.length) {
            return index;
        }
        if (!isPlaceholderCodePoint(codePoints[index])) {
            return index;
        }

        int cursor = index + 1;
        while (cursor < codePoints.length && isPlaceholderDecorator(codePoints[cursor])) {
            cursor++;
        }
        return cursor;
    }

    private boolean isPlaceholderCodePoint(int codePoint) {
        if (codePoint == 0xFFFC || codePoint == 0xFFFD) {
            return true;
        }

        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.PRIVATE_USE_AREA
                || block == Character.UnicodeBlock.SUPPLEMENTARY_PRIVATE_USE_AREA_A
                || block == Character.UnicodeBlock.SUPPLEMENTARY_PRIVATE_USE_AREA_B;
    }

    private boolean isPlaceholderDecorator(int codePoint) {
        return codePoint == 0x200D
                || codePoint == 0xFE0E
                || codePoint == 0xFE0F
                || codePoint == 0x20E3;
    }

    private String codePointsToString(int[] codePoints, int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) {
            return "";
        }
        return new String(codePoints, startInclusive, endExclusive - startInclusive);
    }
}
