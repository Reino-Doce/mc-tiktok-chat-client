package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.config.ChatFormatTemplate;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Applies the configured chat format template while preserving rich inline media segments.
 */
final class LiveMessageTemplateRenderer {
    private static final String DEFAULT_STAR_CHAT_FORMAT = ChatFormatTemplate.PREFIX_TOKEN
            + " <" + ChatFormatTemplate.USERNAME_TOKEN + "> " + ChatFormatTemplate.MESSAGE_TOKEN;
    private static final String STAR_MARKER = " \u2b50 STAR";

    private final InlineMediaTokenRegistry tokenRegistry;

    LiveMessageTemplateRenderer(InlineMediaTokenRegistry tokenRegistry) {
        this.tokenRegistry = tokenRegistry;
    }

    void appendTemplate(
            MutableComponent line,
            ChatMessageStyle style,
            RichLiveMessage message,
            ChatFormatting bodyColor,
            boolean starComment
    ) {
        ChatMessageStyle safeStyle = normalizeStyle(style);
        String format = starComment && ReinodoceConfig.DEFAULT_CHAT_FORMAT.equals(safeStyle.format())
                ? DEFAULT_STAR_CHAT_FORMAT
                : safeStyle.format();
        appendTemplateParts(new RenderContext(line, safeStyle, message, bodyColor, starComment), format);
    }

    private ChatMessageStyle normalizeStyle(ChatMessageStyle style) {
        if (style == null) {
            return new ChatMessageStyle(ReinodoceConfig.DEFAULT_CHAT_PREFIX, ReinodoceConfig.DEFAULT_CHAT_FORMAT);
        }
        return style;
    }

    private void appendTemplateParts(RenderContext context, String format) {
        int offset = 0;
        while (offset < format.length()) {
            int tokenIndex = nextTokenIndex(format, offset);
            if (tokenIndex < 0) {
                appendLiteral(context.line(), format.substring(offset));
                return;
            }
            appendLiteral(context.line(), format.substring(offset, tokenIndex));
            offset = appendToken(context, format, tokenIndex);
        }
    }

    private int nextTokenIndex(String format, int offset) {
        int prefixIndex = format.indexOf(ChatFormatTemplate.PREFIX_TOKEN, offset);
        int usernameIndex = format.indexOf(ChatFormatTemplate.USERNAME_TOKEN, offset);
        int messageIndex = format.indexOf(ChatFormatTemplate.MESSAGE_TOKEN, offset);
        return minPositive(prefixIndex, minPositive(usernameIndex, messageIndex));
    }

    private int appendToken(RenderContext context, String format, int tokenIndex) {
        if (format.startsWith(ChatFormatTemplate.PREFIX_TOKEN, tokenIndex)) {
            context.line().append(Component.literal(context.style().prefix()).withStyle(ChatFormatting.YELLOW));
            appendStarMarker(context.line(), context.starComment());
            return tokenIndex + ChatFormatTemplate.PREFIX_TOKEN.length();
        }
        if (format.startsWith(ChatFormatTemplate.USERNAME_TOKEN, tokenIndex)) {
            appendSegments(context.line(), context.message().authorSegments(), ChatFormatting.WHITE);
            return tokenIndex + ChatFormatTemplate.USERNAME_TOKEN.length();
        }
        appendSegments(context.line(), context.message().bodySegments(), context.bodyColor());
        return tokenIndex + ChatFormatTemplate.MESSAGE_TOKEN.length();
    }

    private int minPositive(int first, int second) {
        if (first < 0) {
            return second;
        }
        if (second < 0) {
            return first;
        }
        return Math.min(first, second);
    }

    private void appendLiteral(MutableComponent line, String literal) {
        if (!literal.isEmpty()) {
            line.append(Component.literal(literal).withStyle(ChatFormatting.WHITE));
        }
    }

    private void appendSegments(MutableComponent line, List<RichLiveMessage.Segment> segments, ChatFormatting color) {
        for (RichLiveMessage.Segment segment : segments) {
            if (segment instanceof RichLiveMessage.TextSegment textSegment) {
                line.append(Component.literal(textSegment.text()).withStyle(color));
                continue;
            }
            if (segment instanceof RichLiveMessage.InlineMediaSegment inlineMediaSegment) {
                line.append(Component.literal(tokenRegistry.tokenFor(inlineMediaSegment)).withStyle(color));
            }
        }
    }

    private void appendStarMarker(MutableComponent line, boolean starComment) {
        if (starComment) {
            line.append(Component.literal(STAR_MARKER).withStyle(ChatFormatting.GOLD));
        }
    }

    private record RenderContext(
            MutableComponent line,
            ChatMessageStyle style,
            RichLiveMessage message,
            ChatFormatting bodyColor,
            boolean starComment
    ) {
    }
}
