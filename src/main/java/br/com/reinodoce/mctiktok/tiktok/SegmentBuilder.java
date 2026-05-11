package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import io.github.jwdeveloper.tiktok.messages.data.Emote;
import io.github.jwdeveloper.tiktok.messages.data.Image;

import java.util.ArrayList;
import java.util.List;

final class SegmentBuilder {
    private static final String EMOTE_FALLBACK = "[emote]";

    private final List<RichLiveMessage.Segment> segments = new ArrayList<>();

    List<RichLiveMessage.Segment> build() {
        return segments;
    }

    boolean isEmpty() {
        return segments.isEmpty();
    }

    void appendText(String rawText) {
        String sanitized = MessageTextUtils.sanitizeSegmentText(rawText);
        if (sanitized.isBlank()) {
            return;
        }
        if (mergeIntoTrailingTextSegment(sanitized)) {
            return;
        }
        segments.add(new RichLiveMessage.TextSegment(sanitized));
    }

    void appendEmote(Emote emote) {
        if (emote == null || !emote.hasImage()) {
            return;
        }
        appendEmote(emote.getEmoteId(), emote.getImage());
    }

    void appendEmote(Image image) {
        appendEmote("", image);
    }

    void appendEmote(String emoteId, Image image) {
        String url = TikTokMediaResolver.resolveImageUrl(image);
        if (url.isBlank()) {
            return;
        }
        segments.add(new RichLiveMessage.RemoteEmoteSegment(emoteId, url, EMOTE_FALLBACK));
    }

    private boolean mergeIntoTrailingTextSegment(String sanitized) {
        if (segments.isEmpty()) {
            return false;
        }
        RichLiveMessage.Segment last = segments.get(segments.size() - 1);
        if (!(last instanceof RichLiveMessage.TextSegment existing)) {
            return false;
        }
        segments.set(segments.size() - 1, new RichLiveMessage.TextSegment(existing.text() + sanitized));
        return true;
    }
}
