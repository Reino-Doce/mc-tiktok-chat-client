package br.com.reinodoce.mctiktok.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Formats command results and status lines for the `/reinodoce` command surface.
 */
public final class CommandFeedback {
    private CommandFeedback() {
    }

    /**
     * Sends a command result to the command source using success or failure styling.
     *
     * @param source command source to receive feedback
     * @param result command result to display
     */
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

    /**
     * Sends a command result and adapts it to Brigadier's integer return convention.
     *
     * @param source command source to receive feedback
     * @param result command result to display
     * @return {@code 1} for success, otherwise {@code 0}
     */
    public static int sendResult(CommandSourceStack source, CommandResult result) {
        send(source, result);
        return result.success() ? 1 : 0;
    }

    /**
     * Sends one neutral status line to the command source.
     *
     * @param source command source to receive feedback
     * @param line status line body
     */
    public static void sendLine(CommandSourceStack source, String line) {
        source.sendSuccess(() -> Component.literal("[ReinoDoce] ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(line).withStyle(ChatFormatting.GRAY)), false);
    }
}
