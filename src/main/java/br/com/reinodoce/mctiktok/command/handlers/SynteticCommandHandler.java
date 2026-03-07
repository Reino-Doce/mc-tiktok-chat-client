package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.client.ReinodoceClientBootstrap;
import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.CommandResult;
import net.minecraft.commands.CommandSourceStack;

public final class SynteticCommandHandler {
    private SynteticCommandHandler() {
    }

    public static int gift(CommandSourceStack source, int value) {
        CommandResult result = ReinodoceClientBootstrap.service().setSynteticGift(value);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }

    public static int giftCombo(CommandSourceStack source, String mode) {
        CommandResult result = ReinodoceClientBootstrap.service().setSynteticGiftComboMode(mode);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }

    public static int follow(CommandSourceStack source, boolean enabled) {
        CommandResult result = ReinodoceClientBootstrap.service().setSynteticFollow(enabled);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }

    public static int join(CommandSourceStack source, boolean enabled) {
        CommandResult result = ReinodoceClientBootstrap.service().setSynteticJoin(enabled);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }

    public static int memberLevel(CommandSourceStack source, boolean enabled) {
        CommandResult result = ReinodoceClientBootstrap.service().setSynteticMemberLevel(enabled);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }
}
