package br.com.reinodoce.mctiktok.command;

import br.com.reinodoce.mctiktok.command.handlers.ConnectCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.DisconnectCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.ReloadCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.RuleCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.SettingsCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.StatusCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.SynteticCommandHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;

import java.util.List;

public final class ReinodoceCommandTree {
    private static final List<String> GIFT_COMBO_MODES = List.of("ignore", "single", "bulk");

    private ReinodoceCommandTree() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reinodoce")
                .executes(ctx -> StatusCommandHandler.execute(ctx.getSource()))
                .then(Commands.literal("connect")
                        .then(Commands.argument("username", StringArgumentType.word())
                                .executes(ctx -> ConnectCommandHandler.execute(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "username")
                                ))))
                .then(Commands.literal("disconnect")
                        .executes(ctx -> DisconnectCommandHandler.execute(ctx.getSource())))
                .then(Commands.literal("status")
                        .executes(ctx -> StatusCommandHandler.execute(ctx.getSource())))
                .then(Commands.literal("settings")
                        .then(Commands.literal("reconnect")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                        .executes(ctx -> SettingsCommandHandler.reconnect(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds")
                                        )))))
                .then(Commands.literal("rule")
                        .then(Commands.literal("follower")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> RuleCommandHandler.follower(
                                                ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "enabled")
                                        ))))
                        .then(Commands.literal("min-member-level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                        .executes(ctx -> RuleCommandHandler.minMemberLevel(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "level")
                                        )))))
                .then(Commands.literal("syntetic")
                        .then(Commands.literal("gift")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> SynteticCommandHandler.gift(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "value")
                                        ))))
                        .then(Commands.literal("gift-combo")
                                .then(Commands.argument("mode", StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(GIFT_COMBO_MODES, builder))
                                        .executes(ctx -> SynteticCommandHandler.giftCombo(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mode")
                                        ))))
                        .then(Commands.literal("follow")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> SynteticCommandHandler.follow(
                                                ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "enabled")
                                        ))))
                        .then(Commands.literal("join")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> SynteticCommandHandler.join(
                                                ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "enabled")
                                        ))))
                        .then(Commands.literal("member-level")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> SynteticCommandHandler.memberLevel(
                                                ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "enabled")
                                        )))))
                .then(Commands.literal("reload")
                        .executes(ctx -> ReloadCommandHandler.execute(ctx.getSource())));
    }
}
