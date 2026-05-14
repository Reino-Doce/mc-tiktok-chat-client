package br.com.reinodoce.mctiktok.client.pinned;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.pinned.PinnedLiveMessage;
import br.com.reinodoce.mctiktok.pinned.PinnedMessageSink;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Objects;

/**
 * Client-only pinned-message sink that writes into the local overlay store.
 */
public class MinecraftPinnedMessageGateway implements PinnedMessageSink {
    private final MinecraftPlatformBridge platformBridge;
    private final PinnedMessageStore messageStore;

    /**
     * Creates a pinned-message gateway.
     *
     * @param platformBridge Minecraft client adapter
     * @param messageStore pinned overlay store
     */
    public MinecraftPinnedMessageGateway(MinecraftPlatformBridge platformBridge, PinnedMessageStore messageStore) {
        this.platformBridge = Objects.requireNonNull(platformBridge, "platformBridge");
        this.messageStore = Objects.requireNonNull(messageStore, "messageStore");
    }

    @Override
    public void showPinnedMessage(ReinodoceConfig config, PinnedLiveMessage message) {
        if (config == null || message == null || message.message().isBlank()) {
            return;
        }
        Component component = format(message);
        platformBridge.runOnClientThread(() -> messageStore.add(
                message.pinId(),
                component,
                message.duration(),
                config.getPinnedOverlayMessages()));
    }

    @Override
    public void clearPinnedMessages() {
        platformBridge.runOnClientThread(messageStore::clear);
    }

    private static Component format(PinnedLiveMessage message) {
        MutableComponent component = Component.empty();
        component.append(Component.translatable("reinodoce.pinned.label").withStyle(ChatFormatting.GOLD));
        component.append(Component.literal(" @").withStyle(ChatFormatting.DARK_AQUA));
        component.append(Component.literal(message.username()).withStyle(ChatFormatting.WHITE));
        component.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
        component.append(Component.literal(message.message()).withStyle(ChatFormatting.GRAY));
        return component;
    }
}
