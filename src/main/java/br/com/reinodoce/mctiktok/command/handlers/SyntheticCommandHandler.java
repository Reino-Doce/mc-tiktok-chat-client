package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce synthetic ...` command execution.
 */
public final class SyntheticCommandHandler {
    private SyntheticCommandHandler() {
    }

    /**
     * Updates the minimum diamond value for synthetic gift output.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param value minimum gift value
     * @return Brigadier command result code
     */
    public static int gift(ReinodoceCommandService service, CommandSourceStack source, int value) {
        return CommandFeedback.sendResult(source, service.setSyntheticGift(value));
    }

    /**
     * Updates synthetic gift combo aggregation mode.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param mode combo mode identifier
     * @return Brigadier command result code
     */
    public static int giftCombo(ReinodoceCommandService service, CommandSourceStack source, String mode) {
        return CommandFeedback.sendResult(source, service.setSyntheticGiftComboMode(mode));
    }

    /**
     * Updates synthetic follow output.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether synthetic follows should be emitted
     * @return Brigadier command result code
     */
    public static int follow(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSyntheticFollow(enabled));
    }

    /**
     * Updates synthetic join output.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether synthetic joins should be emitted
     * @return Brigadier command result code
     */
    public static int join(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSyntheticJoin(enabled));
    }

    /**
     * Updates synthetic member-level output.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether member-level messages should be emitted
     * @return Brigadier command result code
     */
    public static int memberLevel(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setSyntheticMemberLevel(enabled));
    }
}
