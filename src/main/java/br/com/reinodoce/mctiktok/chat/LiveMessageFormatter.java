package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.i18n.Translations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class LiveMessageFormatter {
    private final InlineMediaTokenRegistry tokenRegistry;

    public LiveMessageFormatter(InlineMediaTokenRegistry tokenRegistry) {
        this.tokenRegistry = tokenRegistry;
    }

    public Component formatLiveComment(String prefix, String username, String message) {
        return formatPlainComment(prefix, username, message, false);
    }

    public FormattedLiveComment formatLiveComment(String prefix, RichLiveMessage message) {
        return formatRichComment(prefix, message, false);
    }

    public Component formatStarComment(String prefix, String username, String message) {
        return formatPlainComment(prefix, username, message, true);
    }

    public FormattedLiveComment formatStarComment(String prefix, RichLiveMessage message) {
        return formatRichComment(prefix, message, true);
    }

    public FormattedLiveComment formatSyntheticGift(String prefix, RichLiveMessage message) {
        return formatRichLine(prefix, message, ChatFormatting.LIGHT_PURPLE);
    }

    public FormattedLiveComment formatSyntheticFollow(String prefix, RichLiveMessage message) {
        return formatRichLine(
                prefix,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.follow")))),
                ChatFormatting.GREEN
        );
    }

    public FormattedLiveComment formatSyntheticJoin(String prefix, RichLiveMessage message) {
        return formatRichLine(
                prefix,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.join")))),
                ChatFormatting.AQUA
        );
    }

    public FormattedLiveComment formatSyntheticMemberLevel(String prefix, RichLiveMessage message, int memberLevel) {
        return formatRichLine(
                prefix,
                message.withBodySegments(List.of(new RichLiveMessage.TextSegment(translate("reinodoce.chat.member_level", memberLevel)))),
                ChatFormatting.GOLD
        );
    }

    public FormattedLiveComment formatRichLine(String prefix, RichLiveMessage message, ChatFormatting bodyColor) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <").withStyle(ChatFormatting.WHITE));
        appendSegments(line, message.authorSegments(), ChatFormatting.WHITE);
        line.append(Component.literal("> ").withStyle(ChatFormatting.WHITE));
        appendSegments(line, message.bodySegments(), bodyColor);
        return new FormattedLiveComment(line, message);
    }

    public Component formatSyntheticGift(String prefix, String username, String giftName, int count) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(
                translate("reinodoce.chat.gift_sent_prefix")
                        + giftName
                        + translate("reinodoce.chat.gift_count_suffix", count)
        ).withStyle(ChatFormatting.LIGHT_PURPLE));
        return line;
    }

    public Component formatSyntheticFollow(String prefix, String username) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(translate("reinodoce.chat.follow")).withStyle(ChatFormatting.GREEN));
        return line;
    }

    public Component formatSyntheticJoin(String prefix, String username) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(translate("reinodoce.chat.join")).withStyle(ChatFormatting.AQUA));
        return line;
    }

    public Component formatSyntheticMemberLevel(String prefix, String username, int memberLevel) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(translate("reinodoce.chat.member_level", memberLevel)).withStyle(ChatFormatting.GOLD));
        return line;
    }

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
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        return line;
    }

    private FormattedLiveComment formatRichComment(String prefix, RichLiveMessage message, boolean starComment) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        appendStarMarker(line, starComment);
        line.append(Component.literal(" <").withStyle(ChatFormatting.WHITE));
        appendSegments(line, message.authorSegments(), ChatFormatting.WHITE);
        line.append(Component.literal("> ").withStyle(ChatFormatting.WHITE));
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
        String value = prefix == null || prefix.isBlank() ? "[LIVE]" : prefix;
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
