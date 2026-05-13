package br.com.reinodoce.mctiktok.chat;

import net.minecraft.network.chat.Component;

/**
 * Result of rendering a rich live message to a Minecraft component while retaining the source segments.
 *
 * @param component rendered Minecraft chat component
 * @param richMessage source rich message used to build the component
 */
public record FormattedLiveComment(Component component, RichLiveMessage richMessage) {
}
