package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

public final class RuleCommandHandler {
    private RuleCommandHandler() {
    }

    public static int follower(ReinodoceCommandService service, CommandSourceStack source, boolean enabled) {
        return CommandFeedback.sendResult(source, service.setFollowerRule(enabled));
    }

    public static int minMemberLevel(ReinodoceCommandService service, CommandSourceStack source, int level) {
        return CommandFeedback.sendResult(source, service.setMinMemberLevelRule(level));
    }
}
