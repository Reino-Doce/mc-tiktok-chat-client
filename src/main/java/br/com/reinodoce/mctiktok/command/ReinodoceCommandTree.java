package br.com.reinodoce.mctiktok.command;

import br.com.reinodoce.mctiktok.command.handlers.ConnectCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.DisconnectCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.ReloadCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.RuleCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.SettingsCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.StatusCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.SynteticCommandHandler;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.List;

public final class ReinodoceCommandTree {
    private static final List<String> GIFT_COMBO_MODES = List.of("ignore", "single", "bulk");
    private static final String ARG_ENABLED = "enabled";

    private ReinodoceCommandTree() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reinodoce")
                .executes(ctx -> StatusCommandHandler.execute(ctx.getSource()))
                .then(connectBranch())
                .then(disconnectBranch())
                .then(statusBranch())
                .then(settingsBranch())
                .then(ruleBranch())
                .then(synteticBranch())
                .then(reloadBranch());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> connectBranch() {
        return Commands.literal("connect")
                .then(Commands.argument("username", StringArgumentType.word())
                        .executes(ctx -> ConnectCommandHandler.execute(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "username"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> disconnectBranch() {
        return Commands.literal("disconnect")
                .executes(ctx -> DisconnectCommandHandler.execute(ctx.getSource()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statusBranch() {
        return Commands.literal("status")
                .executes(ctx -> StatusCommandHandler.execute(ctx.getSource()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> settingsBranch() {
        return Commands.literal("settings")
                .then(Commands.literal("reconnect")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(ctx -> SettingsCommandHandler.reconnect(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "seconds")))))
                .then(Commands.literal("chat-emotes")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> SettingsCommandHandler.chatEmotes(
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ruleBranch() {
        return Commands.literal("rule")
                .then(Commands.literal("follower")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> RuleCommandHandler.follower(
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(Commands.literal("min-member-level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                .executes(ctx -> RuleCommandHandler.minMemberLevel(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> synteticBranch() {
        return Commands.literal("syntetic")
                .then(Commands.literal("gift")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(ctx -> SynteticCommandHandler.gift(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "value")))))
                .then(Commands.literal("gift-combo")
                        .then(Commands.argument("mode", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(GIFT_COMBO_MODES, builder))
                                .executes(ctx -> SynteticCommandHandler.giftCombo(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "mode")))))
                .then(syntheticToggle("follow", SynteticCommandHandler::follow))
                .then(syntheticToggle("join", SynteticCommandHandler::join))
                .then(syntheticToggle("member-level", SynteticCommandHandler::memberLevel));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> syntheticToggle(
            String literal, ToggleHandler handler
    ) {
        return Commands.literal(literal)
                .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                        .executes(ctx -> handler.run(
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, ARG_ENABLED))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> reloadBranch() {
        return Commands.literal("reload")
                .executes(ctx -> ReloadCommandHandler.execute(ctx.getSource()));
    }

    @FunctionalInterface
    private interface ToggleHandler {
        int run(CommandSourceStack source, boolean enabled);
    }
}
