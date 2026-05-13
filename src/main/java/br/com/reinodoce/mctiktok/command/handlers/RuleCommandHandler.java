package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce rule ...` command execution.
 */
public final class RuleCommandHandler {
    private RuleCommandHandler() {
    }

    /**
     * Updates the follower-only display rule.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param enabled whether follower-only mode should be enabled
     * @return Brigadier command result code
     */
    public static int follower(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setFollowerRule(enabled));
    }

    /**
     * Updates the minimum member-level display rule.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param level minimum allowed member level
     * @return Brigadier command result code
     */
    public static int minMemberLevel(ReinodoceCommandService service, CommandSourceStack source, int level) {
        return CommandFeedback.sendResult(source, service.setMinMemberLevelRule(level));
    }
}
