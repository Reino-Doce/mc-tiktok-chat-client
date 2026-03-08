package br.com.reinodoce.mctiktok.emoji;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;

import java.util.ArrayList;
import java.util.List;

public final class UnicodeEmojiParser {
    private final UnicodeEmojiCatalog catalog;

    public UnicodeEmojiParser() {
        this(UnicodeEmojiCatalog.getInstance());
    }

    UnicodeEmojiParser(UnicodeEmojiCatalog catalog) {
        this.catalog = catalog;
    }

    public RichLiveMessage expand(RichLiveMessage input) {
        if (input == null) {
            return input;
        }

        return new RichLiveMessage(
                input.messageId(),
                expandSegments(input.authorSegments()),
                expandSegments(input.bodySegments())
        );
    }

    public List<RichLiveMessage.Segment> parseText(String text) {
        List<RichLiveMessage.Segment> output = new ArrayList<>();
        appendExpandedText(output, text);
        return List.copyOf(output);
    }

    public List<RichLiveMessage.Segment> expandSegments(List<RichLiveMessage.Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }

        List<RichLiveMessage.Segment> expanded = new ArrayList<>();
        for (RichLiveMessage.Segment segment : segments) {
            if (segment instanceof RichLiveMessage.TextSegment textSegment) {
                appendExpandedText(expanded, textSegment.text());
                continue;
            }
            expanded.add(segment);
        }
        return List.copyOf(expanded);
    }

    private void appendExpandedText(List<RichLiveMessage.Segment> output, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int[] codePoints = text.codePoints().toArray();
        StringBuilder plainText = new StringBuilder(text.length());
        int index = 0;
        while (index < codePoints.length) {
            UnicodeEmojiCatalog.Match match = catalog.longestMatch(codePoints, index);
            if (match == null) {
                plainText.appendCodePoint(codePoints[index]);
                index++;
                continue;
            }

            flushText(output, plainText);
            int[] matched = new int[match.endExclusive() - match.startInclusive()];
            System.arraycopy(codePoints, match.startInclusive(), matched, 0, matched.length);
            String emojiText = new String(matched, 0, matched.length);
            output.add(new RichLiveMessage.UnicodeEmojiSegment(
                    emojiText,
                    TwemojiUrlResolver.toIconId(matched),
                    emojiText
            ));
            index = match.endExclusive();
        }

        flushText(output, plainText);
    }

    private void flushText(List<RichLiveMessage.Segment> output, StringBuilder plainText) {
        if (plainText.isEmpty()) {
            return;
        }
        output.add(new RichLiveMessage.TextSegment(plainText.toString()));
        plainText.setLength(0);
    }
}
