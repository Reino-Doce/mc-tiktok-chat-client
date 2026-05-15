package br.com.reinodoce.mctiktok.command;

import br.com.reinodoce.mctiktok.command.handlers.RuleCommandHandler;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class ReinodoceRuleCommandTree {
    private static final String ARG_ENABLED = "enabled";
    private static final String ARG_SECONDS = "seconds";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_VALUE = "value";

    private ReinodoceRuleCommandTree() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> branch(ReinodoceCommandService service) {
        return Commands.literal("rule")
                .then(followerRuleBranch(service))
                .then(minMemberLevelRuleBranch(service))
                .then(blockWordBranch(service))
                .then(blockUserBranch(service))
                .then(emoteOnlyRuleBranch(service))
                .then(linkRuleBranch(service))
                .then(userCooldownRuleBranch(service))
                .then(allowlistBranch(service))
                .then(maxLengthRuleBranch(service))
                .then(duplicateCooldownRuleBranch(service));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> followerRuleBranch(ReinodoceCommandService service) {
        return Commands.literal("follower")
                .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                        .executes(ctx -> RuleCommandHandler.follower(
                                service,
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, ARG_ENABLED))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> minMemberLevelRuleBranch(
            ReinodoceCommandService service
    ) {
        return Commands.literal("min-member-level")
                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                        .executes(ctx -> RuleCommandHandler.minMemberLevel(
                                service,
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "level"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> blockWordBranch(ReinodoceCommandService service) {
        return Commands.literal("block-word")
                .then(Commands.literal("add")
                        .then(Commands.argument(ARG_VALUE, StringArgumentType.greedyString())
                                .executes(ctx -> RuleCommandHandler.addBlockedWord(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_VALUE)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument(ARG_VALUE, StringArgumentType.greedyString())
                                .executes(ctx -> RuleCommandHandler.removeBlockedWord(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_VALUE)))))
                .then(Commands.literal("import")
                        .then(Commands.argument(ARG_VALUE, StringArgumentType.greedyString())
                                .executes(ctx -> RuleCommandHandler.importBlockedWords(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_VALUE)))))
                .then(Commands.literal("export")
                        .executes(ctx -> RuleCommandHandler.exportBlockedWords(service, ctx.getSource())))
                .then(Commands.literal("list")
                        .executes(ctx -> RuleCommandHandler.listBlockedWords(service, ctx.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> blockUserBranch(ReinodoceCommandService service) {
        return Commands.literal("block-user")
                .then(Commands.literal("add")
                        .then(Commands.argument(ARG_USERNAME, StringArgumentType.word())
                                .executes(ctx -> RuleCommandHandler.addBlockedUser(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_USERNAME)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument(ARG_USERNAME, StringArgumentType.word())
                                .executes(ctx -> RuleCommandHandler.removeBlockedUser(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_USERNAME)))))
                .then(Commands.literal("import")
                        .then(Commands.argument(ARG_VALUE, StringArgumentType.greedyString())
                                .executes(ctx -> RuleCommandHandler.importBlockedUsers(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_VALUE)))))
                .then(Commands.literal("export")
                        .executes(ctx -> RuleCommandHandler.exportBlockedUsers(service, ctx.getSource())))
                .then(Commands.literal("list")
                        .executes(ctx -> RuleCommandHandler.listBlockedUsers(service, ctx.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> emoteOnlyRuleBranch(ReinodoceCommandService service) {
        return Commands.literal("emote-only")
                .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                        .executes(ctx -> RuleCommandHandler.emoteOnly(
                                service,
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, ARG_ENABLED))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> linkRuleBranch(ReinodoceCommandService service) {
        return Commands.literal("links")
                .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                        .executes(ctx -> RuleCommandHandler.links(
                                service,
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, ARG_ENABLED))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> userCooldownRuleBranch(
            ReinodoceCommandService service
    ) {
        return Commands.literal("user-cooldown")
                .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(0))
                        .executes(ctx -> RuleCommandHandler.userCooldown(
                                service,
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, ARG_SECONDS))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> allowlistBranch(ReinodoceCommandService service) {
        return Commands.literal("allowlist")
                .then(Commands.literal(ARG_ENABLED)
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> RuleCommandHandler.allowlistMode(
                                        service,
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(Commands.literal("add")
                        .then(Commands.argument(ARG_USERNAME, StringArgumentType.word())
                                .executes(ctx -> RuleCommandHandler.addAllowedUser(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_USERNAME)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument(ARG_USERNAME, StringArgumentType.word())
                                .executes(ctx -> RuleCommandHandler.removeAllowedUser(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_USERNAME)))))
                .then(Commands.literal("list")
                        .executes(ctx -> RuleCommandHandler.listAllowedUsers(service, ctx.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> maxLengthRuleBranch(ReinodoceCommandService service) {
        return Commands.literal("max-length")
                .then(Commands.argument("length", IntegerArgumentType.integer(0))
                        .executes(ctx -> RuleCommandHandler.maxLength(
                                service,
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "length"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> duplicateCooldownRuleBranch(
            ReinodoceCommandService service
    ) {
        return Commands.literal("duplicate-cooldown")
                .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(0))
                        .executes(ctx -> RuleCommandHandler.duplicateCooldown(
                                service,
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, ARG_SECONDS))));
    }
}
