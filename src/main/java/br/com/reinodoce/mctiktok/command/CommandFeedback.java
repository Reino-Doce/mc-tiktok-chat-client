package br.com.reinodoce.mctiktok.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class CommandFeedback {
    private CommandFeedback() {
    }

    public static void send(CommandSourceStack source, CommandResult result) {
        MutableComponent message = Component.empty();
        message.append(Component.literal("[ReinoDoce] ").withStyle(ChatFormatting.DARK_AQUA));
        message.append(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (result.success()) {
            source.sendSuccess(() -> message, false);
        } else {
            source.sendFailure(message);
        }
    }

    public static int sendResult(CommandSourceStack source, CommandResult result) {
        send(source, result);
        return result.success() ? 1 : 0;
    }

    public static void sendLine(CommandSourceStack source, String line) {
        source.sendSuccess(() -> Component.literal("[ReinoDoce] ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(line).withStyle(ChatFormatting.GRAY)), false);
    }
}
