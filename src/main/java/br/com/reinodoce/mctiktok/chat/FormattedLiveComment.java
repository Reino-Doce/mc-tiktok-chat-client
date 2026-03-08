package br.com.reinodoce.mctiktok.chat;

import net.minecraft.network.chat.Component;

public record FormattedLiveComment(Component component, RichLiveMessage richMessage) {
}
