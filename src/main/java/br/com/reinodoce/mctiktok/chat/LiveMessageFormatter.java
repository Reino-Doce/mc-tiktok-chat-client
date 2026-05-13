package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.i18n.Translations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Formats plain and rich TikTok LIVE events into Minecraft chat components.
 */
public class LiveMessageFormatter {
    private static final String AUTHOR_OPEN = " <";
    private static final String AUTHOR_CLOSE = "> ";

    private final InlineMediaTokenRegistry tokenRegistry;

    /**
     * Creates a formatter that can allocate inline media tokens while rendering rich messages.
     *
     * @param tokenRegistry registry used for inline media token insertion
     */
    public LiveMessageFormatter(InlineMediaTokenRegistry tokenRegistry) {
        this.tokenRegistry = tokenRegistry;
    }

    /**
     * Formats a plain live comment.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @param message sanitized message body
     * @return rendered component
     */
    public Component formatLiveComment(String prefix, String username, String message) {
        return formatPlainComment(prefix, username, message, false);
    }

    /**
     * Formats a rich live comment.
     *
     * @param prefix configured chat prefix
     * @param message rich message to render
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatLiveComment(String prefix, RichLiveMessage message) {
        return formatRichComment(prefix, message, false);
    }

    /**
     * Formats a plain highlighted star comment.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @param message sanitized message body
     * @return rendered component
     */
    public Component formatStarComment(String prefix, String username, String message) {
        return formatPlainComment(prefix, username, message, true);
    }

    /**
     * Formats a rich highlighted star comment.
     *
     * @param prefix configured chat prefix
     * @param message rich message to render
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatStarComment(String prefix, RichLiveMessage message) {
        return formatRichComment(prefix, message, true);
    }

    /**
     * Formats a rich synthetic gift line.
     *
     * @param prefix configured chat prefix
     * @param message rich gift message
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatSyntheticGift(String prefix, RichLiveMessage message) {
        return formatRichLine(prefix, message, ChatFormatting.LIGHT_PURPLE);
    }

    /**
     * Formats a rich synthetic follow line.
     *
     * @param prefix configured chat prefix
     * @param message rich user message
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatSyntheticFollow(String prefix, RichLiveMessage message) {
        return formatRichLine(
                prefix,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.follow")))),
                ChatFormatting.GREEN
        );
    }

    /**
     * Formats a rich synthetic join line.
     *
     * @param prefix configured chat prefix
     * @param message rich user message
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatSyntheticJoin(String prefix, RichLiveMessage message) {
        return formatRichLine(
                prefix,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.join")))),
                ChatFormatting.AQUA
        );
    }

    /**
     * Formats a rich synthetic member-level line.
     *
     * @param prefix configured chat prefix
     * @param message rich user message
     * @param memberLevel resolved member level
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatSyntheticMemberLevel(String prefix, RichLiveMessage message, int memberLevel) {
        return formatRichLine(
                prefix,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.member_level", memberLevel)))),
                ChatFormatting.GOLD
        );
    }

    /**
     * Formats a rich line with the supplied body color.
     *
     * @param prefix configured chat prefix
     * @param message rich message to render
     * @param bodyColor color applied to body segments
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatRichLine(String prefix, RichLiveMessage message, ChatFormatting bodyColor) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(AUTHOR_OPEN).withStyle(ChatFormatting.WHITE));
        appendSegments(line, message.authorSegments(), ChatFormatting.WHITE);
        line.append(Component.literal(AUTHOR_CLOSE).withStyle(ChatFormatting.WHITE));
        appendSegments(line, message.bodySegments(), bodyColor);
        return new FormattedLiveComment(line, message);
    }

    /**
     * Formats a plain synthetic gift line.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @param giftName gift display name
     * @param count gift count or combo count
     * @return rendered component
     */
    public Component formatSyntheticGift(String prefix, String username, String giftName, int count) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(AUTHOR_OPEN + username + AUTHOR_CLOSE).withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(
                translate("reinodoce.chat.gift_sent_prefix")
                        + giftName
                        + translate("reinodoce.chat.gift_count_suffix", count)
        ).withStyle(ChatFormatting.LIGHT_PURPLE));
        return line;
    }

    /**
     * Formats a plain synthetic follow line.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @return rendered component
     */
    public Component formatSyntheticFollow(String prefix, String username) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(AUTHOR_OPEN + username + AUTHOR_CLOSE).withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(translate("reinodoce.chat.follow")).withStyle(ChatFormatting.GREEN));
        return line;
    }

    /**
     * Formats a plain synthetic join line.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @return rendered component
     */
    public Component formatSyntheticJoin(String prefix, String username) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(AUTHOR_OPEN + username + AUTHOR_CLOSE).withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(translate("reinodoce.chat.join")).withStyle(ChatFormatting.AQUA));
        return line;
    }

    /**
     * Formats a plain synthetic member-level line.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @param memberLevel resolved member level
     * @return rendered component
     */
    public Component formatSyntheticMemberLevel(String prefix, String username, int memberLevel) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(AUTHOR_OPEN + username + AUTHOR_CLOSE).withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(translate("reinodoce.chat.member_level", memberLevel)).withStyle(ChatFormatting.GOLD));
        return line;
    }

    /**
     * Formats an operator-facing system line.
     *
     * @param message status or error text
     * @param success whether the line should use success styling
     * @return rendered component
     */
    public Component formatSystem(String message, boolean success) {
        MutableComponent line = Component.empty();
        line.append(Component.literal("[ReinoDoce] ").withStyle(ChatFormatting.DARK_AQUA));
        line.append(Component.literal(message).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
        return line;
    }

    private Component formatPlainComment(String prefix, String username, String message, boolean starComment) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        appendStarMarker(line, starComment);
        line.append(Component.literal(AUTHOR_OPEN + username + AUTHOR_CLOSE).withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        return line;
    }

    private FormattedLiveComment formatRichComment(String prefix, RichLiveMessage message, boolean starComment) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        appendStarMarker(line, starComment);
        line.append(Component.literal(AUTHOR_OPEN).withStyle(ChatFormatting.WHITE));
        appendSegments(line, message.authorSegments(), ChatFormatting.WHITE);
        line.append(Component.literal(AUTHOR_CLOSE).withStyle(ChatFormatting.WHITE));
        appendSegments(line, message.bodySegments(), ChatFormatting.GRAY);
        return new FormattedLiveComment(line, message);
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

    private Component prefix(String prefix) {
        String value = prefix == null || prefix.isBlank() ? ReinodoceConfig.DEFAULT_CHAT_PREFIX : prefix;
        return Component.literal(value + " ").withStyle(ChatFormatting.YELLOW);
    }

    private void appendStarMarker(MutableComponent line, boolean starComment) {
        if (starComment) {
            line.append(Component.literal("\u2b50 STAR").withStyle(ChatFormatting.GOLD));
        }
    }

    private String translate(String key, Object... args) {
        return Translations.tr(key, args);
    }
}
