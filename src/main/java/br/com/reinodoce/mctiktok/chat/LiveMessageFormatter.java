package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.i18n.Translations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Formats plain and rich TikTok LIVE events into Minecraft chat components.
 */
public class LiveMessageFormatter {
    private final LiveMessageTemplateRenderer templateRenderer;

    /**
     * Creates a formatter that can allocate inline media tokens while rendering rich messages.
     *
     * @param tokenRegistry registry used for inline media token insertion
     */
    public LiveMessageFormatter(InlineMediaTokenRegistry tokenRegistry) {
        this.templateRenderer = new LiveMessageTemplateRenderer(tokenRegistry);
    }

    /**
     * Formats a plain live comment.
     *
     * @param style configured chat style
     * @param username display name to show
     * @param message sanitized message body
     * @return rendered component
     */
    public Component formatLiveComment(ChatMessageStyle style, String username, String message) {
        return formatPlainComment(style, username, message, false);
    }

    /**
     * Formats a rich live comment.
     *
     * @param style configured chat style
     * @param message rich message to render
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatLiveComment(ChatMessageStyle style, RichLiveMessage message) {
        return formatRichComment(style, message, false);
    }

    /**
     * Formats a plain highlighted star comment.
     *
     * @param style configured chat style
     * @param username display name to show
     * @param message sanitized message body
     * @return rendered component
     */
    public Component formatStarComment(ChatMessageStyle style, String username, String message) {
        return formatPlainComment(style, username, message, true);
    }

    /**
     * Formats a rich highlighted star comment.
     *
     * @param style configured chat style
     * @param message rich message to render
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatStarComment(ChatMessageStyle style, RichLiveMessage message) {
        return formatRichComment(style, message, true);
    }

    /**
     * Formats a rich synthetic gift line.
     *
     * @param style configured chat style
     * @param message rich gift message
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatSyntheticGift(ChatMessageStyle style, RichLiveMessage message) {
        return formatRichLine(style, message, ChatFormatting.LIGHT_PURPLE);
    }

    /**
     * Formats a rich synthetic follow line.
     *
     * @param style configured chat style
     * @param message rich user message
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatSyntheticFollow(ChatMessageStyle style, RichLiveMessage message) {
        return formatRichLine(
                style,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.follow")))),
                ChatFormatting.GREEN
        );
    }

    /**
     * Formats a rich synthetic join line.
     *
     * @param style configured chat style
     * @param message rich user message
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatSyntheticJoin(ChatMessageStyle style, RichLiveMessage message) {
        return formatRichLine(
                style,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.join")))),
                ChatFormatting.AQUA
        );
    }

    /**
     * Formats a rich synthetic member-level line.
     *
     * @param style configured chat style
     * @param message rich user message
     * @param memberLevel resolved member level
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatSyntheticMemberLevel(ChatMessageStyle style, RichLiveMessage message, int memberLevel) {
        return formatRichLine(
                style,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.member_level", memberLevel)))),
                ChatFormatting.GOLD
        );
    }

    /**
     * Formats a rich line with the supplied body color.
     *
     * @param style configured chat style
     * @param message rich message to render
     * @param bodyColor color applied to body segments
     * @return rendered component plus source message
     */
    public FormattedLiveComment formatRichLine(ChatMessageStyle style, RichLiveMessage message, ChatFormatting bodyColor) {
        MutableComponent line = Component.empty();
        templateRenderer.appendTemplate(line, style, message, bodyColor, false);
        return new FormattedLiveComment(line, message);
    }

    /**
     * Formats a plain synthetic gift line.
     *
     * @param style configured chat style
     * @param username display name to show
     * @param giftName gift display name
     * @param count gift count or combo count
     * @return rendered component
     */
    public Component formatSyntheticGift(ChatMessageStyle style, String username, String giftName, int count) {
        return formatPlainLine(
                style,
                username,
                translate("reinodoce.chat.gift_sent_prefix")
                        + giftName
                        + translate("reinodoce.chat.gift_count_suffix", count),
                ChatFormatting.LIGHT_PURPLE,
                false);
    }

    /**
     * Formats a plain synthetic follow line.
     *
     * @param style configured chat style
     * @param username display name to show
     * @return rendered component
     */
    public Component formatSyntheticFollow(ChatMessageStyle style, String username) {
        return formatPlainLine(style, username, translate("reinodoce.chat.follow"), ChatFormatting.GREEN, false);
    }

    /**
     * Formats a plain synthetic join line.
     *
     * @param style configured chat style
     * @param username display name to show
     * @return rendered component
     */
    public Component formatSyntheticJoin(ChatMessageStyle style, String username) {
        return formatPlainLine(style, username, translate("reinodoce.chat.join"), ChatFormatting.AQUA, false);
    }

    /**
     * Formats a plain synthetic member-level line.
     *
     * @param style configured chat style
     * @param username display name to show
     * @param memberLevel resolved member level
     * @return rendered component
     */
    public Component formatSyntheticMemberLevel(ChatMessageStyle style, String username, int memberLevel) {
        return formatPlainLine(style, username, translate("reinodoce.chat.member_level", memberLevel), ChatFormatting.GOLD, false);
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

    private Component formatPlainComment(ChatMessageStyle style, String username, String message, boolean starComment) {
        return formatPlainLine(style, username, message, ChatFormatting.GRAY, starComment);
    }

    private FormattedLiveComment formatRichComment(ChatMessageStyle style, RichLiveMessage message, boolean starComment) {
        MutableComponent line = Component.empty();
        templateRenderer.appendTemplate(line, style, message, ChatFormatting.GRAY, starComment);
        return new FormattedLiveComment(line, message);
    }

    private Component formatPlainLine(
            ChatMessageStyle style,
            String username,
            String message,
            ChatFormatting bodyColor,
            boolean starComment
    ) {
        MutableComponent line = Component.empty();
        RichLiveMessage richMessage = new RichLiveMessage(0L, username, List.of(new RichLiveMessage.TextSegment(message)));
        templateRenderer.appendTemplate(line, style, richMessage, bodyColor, starComment);
        return line;
    }

    private String translate(String key, Object... args) {
        return Translations.tr(key, args);
    }
}
