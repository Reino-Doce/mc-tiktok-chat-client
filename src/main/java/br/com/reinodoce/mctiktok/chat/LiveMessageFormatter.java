package br.com.reinodoce.mctiktok.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class LiveMessageFormatter {
    public Component formatLiveComment(String prefix, String username, String message) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        return line;
    }

    public Component formatSyntheticGift(String prefix, String username, String giftName, int count) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal("enviou " + giftName + " x" + count).withStyle(ChatFormatting.LIGHT_PURPLE));
        return line;
    }

    public Component formatSyntheticFollow(String prefix, String username) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal("comecou a seguir").withStyle(ChatFormatting.GREEN));
        return line;
    }

    public Component formatSyntheticJoin(String prefix, String username) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal("entrou na live").withStyle(ChatFormatting.AQUA));
        return line;
    }

    public Component formatSyntheticMemberLevel(String prefix, String username, int memberLevel) {
        MutableComponent line = Component.empty();
        line.append(prefix(prefix));
        line.append(Component.literal(" <" + username + "> ").withStyle(ChatFormatting.WHITE));
        line.append(Component.literal("alcancou nivel " + memberLevel + " de membro").withStyle(ChatFormatting.GOLD));
        return line;
    }

    public Component formatSystem(String message, boolean success) {
        MutableComponent line = Component.empty();
        line.append(Component.literal("[ReinoDoce] ").withStyle(ChatFormatting.DARK_AQUA));
        line.append(Component.literal(message).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
        return line;
    }

    private Component prefix(String prefix) {
        String value = prefix == null || prefix.isBlank() ? "[LIVE]" : prefix;
        return Component.literal(value + " ").withStyle(ChatFormatting.YELLOW);
    }
}
