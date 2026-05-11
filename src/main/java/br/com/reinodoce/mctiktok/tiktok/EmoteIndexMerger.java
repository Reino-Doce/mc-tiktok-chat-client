package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastChatMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class EmoteIndexMerger {
    private EmoteIndexMerger() {
    }

    static List<RichLiveMessage.Segment> merge(
            String rawContent,
            List<WebcastChatMessage.EmoteWithIndex> emotesWithIndex,
            SegmentBuilder buffer
    ) {
        String content = rawContent == null ? "" : rawContent;
        boolean hasEmotes = emotesWithIndex != null && !emotesWithIndex.isEmpty();
        if (content.isBlank() && !hasEmotes) {
            return List.of();
        }
        if (!hasEmotes) {
            buffer.appendText(content);
            return buffer.build();
        }
        int[] codePoints = content.codePoints().toArray();
        int cursor = walkEmotes(codePoints, sortedByIndex(emotesWithIndex), buffer);
        if (cursor < codePoints.length) {
            buffer.appendText(MessageTextUtils.codePointsToString(codePoints, cursor, codePoints.length));
        }
        return buffer.build();
    }

    private static List<WebcastChatMessage.EmoteWithIndex> sortedByIndex(
            List<WebcastChatMessage.EmoteWithIndex> emotesWithIndex
    ) {
        List<WebcastChatMessage.EmoteWithIndex> sorted = new ArrayList<>(emotesWithIndex);
        sorted.sort(Comparator.comparingLong(WebcastChatMessage.EmoteWithIndex::getIndex));
        return sorted;
    }

    private static int walkEmotes(
            int[] codePoints,
            List<WebcastChatMessage.EmoteWithIndex> sorted,
            SegmentBuilder buffer
    ) {
        int cursor = 0;
        for (WebcastChatMessage.EmoteWithIndex emoteWithIndex : sorted) {
            int index = (int) Math.max(0, Math.min(codePoints.length, emoteWithIndex.getIndex()));
            if (index > cursor) {
                buffer.appendText(MessageTextUtils.codePointsToString(codePoints, cursor, index));
            }
            buffer.appendEmote(emoteWithIndex.getEmote());
            cursor = MessageTextUtils.skipPlaceholderCodePoints(codePoints, index);
        }
        return cursor;
    }
}
