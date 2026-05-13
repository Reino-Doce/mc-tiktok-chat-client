package br.com.reinodoce.mctiktok.command;

import br.com.reinodoce.mctiktok.command.handlers.ConnectCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.DisconnectCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.ReloadCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.RuleCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.SettingsCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.StatusCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.SynteticCommandHandler;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Objects;

/**
 * Builds the public `/reinodoce` Brigadier command tree.
 */
public final class ReinodoceCommandTree {
    private static final String ARG_ENABLED = "enabled";
    private static final String ARG_TEMPLATE = "template";
    private static final String ARG_VALUE = "value";

    private ReinodoceCommandTree() {
    }

    /**
     * Builds the root command and all subcommands.
     *
     * @param service command service invoked by handlers
     * @return Brigadier root literal
     */
    public static LiteralArgumentBuilder<CommandSourceStack> build(ReinodoceCommandService service) {
        ReinodoceCommandService commandService = Objects.requireNonNull(service, "service");
        return Commands.literal("reinodoce")
                .executes(ctx -> StatusCommandHandler.execute(ctx.getSource(), commandService))
                .then(connectBranch(commandService))
                .then(disconnectBranch(commandService))
                .then(statusBranch(commandService))
                .then(settingsBranch(commandService))
                .then(ruleBranch(commandService))
                .then(synteticBranch(commandService))
                .then(reloadBranch(commandService));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> connectBranch(ReinodoceCommandService service) {
        return Commands.literal("connect")
                .then(Commands.argument("username", StringArgumentType.word())
                        .executes(ctx -> ConnectCommandHandler.execute(
                                service,
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "username"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> disconnectBranch(ReinodoceCommandService service) {
        return Commands.literal("disconnect")
                .executes(ctx -> DisconnectCommandHandler.execute(service, ctx.getSource()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statusBranch(ReinodoceCommandService service) {
        return Commands.literal("status")
                .executes(ctx -> StatusCommandHandler.execute(ctx.getSource(), service));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> settingsBranch(ReinodoceCommandService service) {
        return Commands.literal("settings")
                .then(Commands.literal("reconnect")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(ctx -> SettingsCommandHandler.reconnect(
                                        service,
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "seconds")))))
                .then(Commands.literal("chat-emotes")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> SettingsCommandHandler.chatEmotes(
                                        service,
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(Commands.literal("prefix")
                        .then(Commands.argument(ARG_VALUE, StringArgumentType.greedyString())
                                .executes(ctx -> SettingsCommandHandler.prefix(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_VALUE)))))
                .then(Commands.literal("format")
                        .then(Commands.argument(ARG_TEMPLATE, StringArgumentType.greedyString())
                                .executes(ctx -> SettingsCommandHandler.format(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_TEMPLATE)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ruleBranch(ReinodoceCommandService service) {
        return Commands.literal("rule")
                .then(Commands.literal("follower")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> RuleCommandHandler.follower(
                                        service,
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(Commands.literal("min-member-level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                .executes(ctx -> RuleCommandHandler.minMemberLevel(
                                        service,
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> synteticBranch(ReinodoceCommandService service) {
        return Commands.literal("syntetic")
                .then(Commands.literal("gift")
                        .then(Commands.argument(ARG_VALUE, IntegerArgumentType.integer(0))
                                .executes(ctx -> SynteticCommandHandler.gift(
                                        service,
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, ARG_VALUE)))))
                .then(Commands.literal("gift-combo")
                        .then(Commands.argument("mode", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        GiftComboMode.ids(), builder))
                                .executes(ctx -> SynteticCommandHandler.giftCombo(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "mode")))))
                .then(syntheticToggle(service, "follow", SynteticCommandHandler::follow))
                .then(syntheticToggle(service, "join", SynteticCommandHandler::join))
                .then(syntheticToggle(service, "member-level", SynteticCommandHandler::memberLevel));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> syntheticToggle(
            ReinodoceCommandService service, String literal, ToggleHandler handler
    ) {
        return Commands.literal(literal)
                .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                        .executes(ctx -> handler.run(
                                service,
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, ARG_ENABLED))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> reloadBranch(ReinodoceCommandService service) {
        return Commands.literal("reload")
                .executes(ctx -> ReloadCommandHandler.execute(service, ctx.getSource()));
    }

    @FunctionalInterface
    private interface ToggleHandler {
        int run(ReinodoceCommandService service, CommandSourceStack source, boolean enabled);
    }
}
