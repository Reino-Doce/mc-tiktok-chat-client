package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

public final class SynteticCommandHandler {
    private SynteticCommandHandler() {
    }

    public static int gift(ReinodoceCommandService service, CommandSourceStack source, int value) {
        return CommandFeedback.sendResult(source, service.setSynteticGift(value));
    }

    public static int giftCombo(ReinodoceCommandService service, CommandSourceStack source, String mode) {
        return CommandFeedback.sendResult(source, service.setSynteticGiftComboMode(mode));
    }

    public static int follow(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSynteticFollow(enabled));
    }

    public static int join(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSynteticJoin(enabled));
    }

    public static int memberLevel(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSynteticMemberLevel(enabled));
    }
}
