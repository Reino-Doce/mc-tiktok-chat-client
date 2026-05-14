package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.hud.HudMessageStore;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.config.LanguageSetting;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat sink implementation that formats TikTok events and schedules Minecraft chat writes on the client thread.
 */
public class MinecraftChatGateway implements ChatEventSink {
    private final MinecraftPlatformBridge platformBridge;
    private final LiveMessageFormatter formatter;
    private final InlineMediaCache inlineMediaCache;
    private final HudMessageStore hudMessageStore;

    /**
     * Creates a gateway.
     *
     * @param platformBridge Minecraft client adapter
     * @param formatter chat formatter
     * @param inlineMediaCache inline media cache used for rich message prefetching
     * @param hudMessageStore HUD output message store
     */
    public MinecraftChatGateway(
            MinecraftPlatformBridge platformBridge,
            LiveMessageFormatter formatter,
            InlineMediaCache inlineMediaCache,
            HudMessageStore hudMessageStore
    ) {
        this.platformBridge = platformBridge;
        this.formatter = formatter;
        this.inlineMediaCache = inlineMediaCache;
        this.hudMessageStore = hudMessageStore;
    }

    @Override
    public void sendLiveComment(ReinodoceConfig config, String username, String message) {
        send(config, formatter.formatLiveComment(ChatMessageStyle.from(config), username, message));
    }

    @Override
    public void sendLiveComment(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatLiveComment(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendStarComment(ReinodoceConfig config, String username, String message) {
        send(config, formatter.formatStarComment(ChatMessageStyle.from(config), username, message));
    }

    @Override
    public void sendStarComment(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatStarComment(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendPinnedComment(ReinodoceConfig config, String username, String message) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        RichLiveMessage pinnedMessage = new RichLiveMessage(
                0L,
                username,
                List.of(new RichLiveMessage.TextSegment(pinnedPrefix(config) + message)));
        send(config, formatter.formatRichLine(style, pinnedMessage, ChatFormatting.GOLD).component());
    }

    @Override
    public void sendPinnedComment(ReinodoceConfig config, RichLiveMessage message) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        sendTracked(config, formatter.formatRichLine(style, withPinnedPrefix(config, message), ChatFormatting.GOLD));
    }

    @Override
    public void sendSyntheticGift(ReinodoceConfig config, String username, String giftName, int count) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        send(config, formatter.formatSyntheticGift(style, username, giftName, count, fixedTextLanguage(config)));
    }

    @Override
    public void sendSyntheticGift(ReinodoceConfig config, RichLiveMessage message) {
        sendTracked(config, formatter.formatSyntheticGift(ChatMessageStyle.from(config), message));
    }

    @Override
    public void sendSyntheticFollow(ReinodoceConfig config, String username) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        send(config, formatter.formatSyntheticFollow(style, username, fixedTextLanguage(config)));
    }

    @Override
    public void sendSyntheticFollow(ReinodoceConfig config, RichLiveMessage message) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        sendTracked(config, formatter.formatSyntheticFollow(style, message, fixedTextLanguage(config)));
    }

    @Override
    public void sendSyntheticJoin(ReinodoceConfig config, String username) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        send(config, formatter.formatSyntheticJoin(style, username, fixedTextLanguage(config)));
    }

    @Override
    public void sendSyntheticJoin(ReinodoceConfig config, RichLiveMessage message) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        sendTracked(config, formatter.formatSyntheticJoin(style, message, fixedTextLanguage(config)));
    }

    @Override
    public void sendSyntheticMemberLevel(ReinodoceConfig config, String username, int memberLevel) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        send(config, formatter.formatSyntheticMemberLevel(style, username, memberLevel, fixedTextLanguage(config)));
    }

    @Override
    public void sendSyntheticMemberLevel(ReinodoceConfig config, RichLiveMessage message, int memberLevel) {
        ChatMessageStyle style = ChatMessageStyle.from(config);
        sendTracked(config, formatter.formatSyntheticMemberLevel(style, message, memberLevel, fixedTextLanguage(config)));
    }

    @Override
    public void sendSystem(String message, boolean success) {
        send(formatter.formatSystem(message, success), true);
    }

    /**
     * Sends a preformatted component to Minecraft chat on the client thread.
     *
     * @param component component to add to chat
     */
    public void send(Component component) {
        send(component, true);
    }

    private void send(Component component, boolean logToChat) {
        platformBridge.runOnClientThread(() -> platformBridge.addChatMessage(component, logToChat));
    }

    private void send(ReinodoceConfig config, Component component) {
        OutputMode mode = OutputMode.fromString(config == null ? "" : config.getOutputMode());
        Component plainComponent = Component.literal(component.getString());
        switch (mode) {
            case CHAT -> send(component, config != null && config.isChatLogEnabled());
            case ACTIONBAR -> platformBridge.runOnClientThread(() -> platformBridge.showActionBarMessage(plainComponent));
            case HUD -> platformBridge.runOnClientThread(() -> hudMessageStore.add(plainComponent, config.getHudLines()));
            case OFF -> {
            }
            default -> send(component, config != null && config.isChatLogEnabled());
        }
    }

    private void sendTracked(ReinodoceConfig config, FormattedLiveComment formatted) {
        if (formatted == null) {
            return;
        }

        RichLiveMessage richMessage = formatted.richMessage();
        String outputMode = config == null ? "" : config.getOutputMode();
        if (richMessage != null && OutputMode.fromString(outputMode) == OutputMode.CHAT) {
            inlineMediaCache.prefetch(richMessage);
        }

        send(config, formatted.component());
    }

    private String effectiveLanguage(ReinodoceConfig config) {
        String configuredLanguage = config == null ? LanguageSetting.AUTO : config.getLanguage();
        return LanguageSetting.resolveEffective(configuredLanguage, platformBridge.selectedLanguageCode());
    }

    private String fixedTextLanguage(ReinodoceConfig config) {
        String effectiveLanguage = effectiveLanguage(config);
        return useRuntimeLanguage(config) ? FixedTextLanguage.runtime(effectiveLanguage) : effectiveLanguage;
    }

    private boolean useRuntimeLanguage(ReinodoceConfig config) {
        String configuredLanguage = config == null ? LanguageSetting.AUTO : config.getLanguage();
        return LanguageSetting.isAuto(configuredLanguage)
                || LanguageSetting.normalizeLocale(platformBridge.selectedLanguageCode())
                .filter(configuredLanguage::equals)
                .isPresent();
    }

    private RichLiveMessage withPinnedPrefix(ReinodoceConfig config, RichLiveMessage message) {
        List<RichLiveMessage.Segment> bodySegments = new ArrayList<>();
        bodySegments.add(new RichLiveMessage.TextSegment(pinnedPrefix(config)));
        bodySegments.addAll(message.bodySegments());
        return message.withBodySegments(bodySegments);
    }

    private String pinnedPrefix(ReinodoceConfig config) {
        return Translations.trForLanguage(
                FixedTextLanguage.locale(fixedTextLanguage(config)),
                FixedTextLanguage.usesRuntime(fixedTextLanguage(config)),
                "reinodoce.chat.pinned_prefix");
    }
}
