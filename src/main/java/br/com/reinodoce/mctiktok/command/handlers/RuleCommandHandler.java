package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.client.ReinodoceClientBootstrap;
import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.CommandResult;
import net.minecraft.commands.CommandSourceStack;

public final class RuleCommandHandler {
    private RuleCommandHandler() {
    }

    public static int follower(CommandSourceStack source, boolean enabled) {
        CommandResult result = ReinodoceClientBootstrap.service().setFollowerRule(enabled);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }

    public static int minMemberLevel(CommandSourceStack source, int level) {
        CommandResult result = ReinodoceClientBootstrap.service().setMinMemberLevelRule(level);
        CommandFeedback.send(source, result);
        return result.success() ? 1 : 0;
    }
}
