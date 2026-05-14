package br.com.reinodoce.mctiktok.command;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.command.handlers.AlertCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.ConnectCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.DisconnectCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.LoggingCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.ReloadCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.RuleCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.SettingsCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.StatsCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.StatusCommandHandler;
import br.com.reinodoce.mctiktok.command.handlers.SyntheticCommandHandler;
import br.com.reinodoce.mctiktok.config.HudPosition;
import br.com.reinodoce.mctiktok.config.LanguageSetting;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.logging.SessionLogFormat;
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
    private static final String ARG_FORMAT = "format";
    private static final String ARG_TEMPLATE = "template";
    private static final String ARG_VALUE = "value";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_SECONDS = "seconds";
    private static final String ARG_MODE = "mode";
    private static final String ARG_POSITION = "position";
    private static final String ARG_LINES = "lines";
    private static final String ARG_LOCALE = "locale";

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
                .then(statsBranch(commandService))
                .then(settingsBranch(commandService))
                .then(ruleBranch(commandService))
                .then(syntheticBranch(commandService))
                .then(alertBranch(commandService))
                .then(loggingBranch(commandService))
                .then(reloadBranch(commandService));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> connectBranch(ReinodoceCommandService service) {
        return Commands.literal("connect")
                .executes(ctx -> ConnectCommandHandler.executeLast(
                        service,
                        ctx.getSource()))
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

    private static LiteralArgumentBuilder<CommandSourceStack> statsBranch(ReinodoceCommandService service) {
        return Commands.literal("stats")
                .executes(ctx -> StatsCommandHandler.execute(ctx.getSource(), service))
                .then(Commands.literal("reset")
                        .executes(ctx -> StatsCommandHandler.reset(ctx.getSource(), service)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> settingsBranch(ReinodoceCommandService service) {
        return Commands.literal("settings")
                .then(Commands.literal("reconnect")
                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(0))
                                .executes(ctx -> SettingsCommandHandler.reconnect(
                                        service,
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, ARG_SECONDS)))))
                .then(Commands.literal("auto-connect")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> SettingsCommandHandler.autoConnect(
                                        service,
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(languageBranch(service))
                .then(outputBranch(service))
                .then(hudPositionBranch(service))
                .then(hudLinesBranch(service))
                .then(Commands.literal("gui")
                        .executes(ctx -> SettingsCommandHandler.gui(service, ctx.getSource())))
                .then(Commands.literal("chat-emotes")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> SettingsCommandHandler.chatEmotes(
                                        service,
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(Commands.literal("chat-log")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> SettingsCommandHandler.chatLog(
                                        service,
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(Commands.literal("prefix")
                        .then(Commands.argument(ARG_VALUE, StringArgumentType.greedyString())
                                .executes(ctx -> SettingsCommandHandler.prefix(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_VALUE)))))
                .then(Commands.literal(ARG_FORMAT)
                        .then(Commands.argument(ARG_TEMPLATE, StringArgumentType.greedyString())
                                .executes(ctx -> SettingsCommandHandler.format(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_TEMPLATE)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> outputBranch(ReinodoceCommandService service) {
        return Commands.literal("output")
                .then(Commands.argument(ARG_MODE, StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                OutputMode.ids(), builder))
                        .executes(ctx -> SettingsCommandHandler.output(
                                service,
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, ARG_MODE))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> languageBranch(ReinodoceCommandService service) {
        return Commands.literal("language")
                .executes(ctx -> SettingsCommandHandler.language(service, ctx.getSource()))
                .then(Commands.argument(ARG_LOCALE, StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                LanguageSetting.suggestions(), builder))
                        .executes(ctx -> SettingsCommandHandler.language(
                                service,
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, ARG_LOCALE))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> hudPositionBranch(ReinodoceCommandService service) {
        return Commands.literal("hud-position")
                .then(Commands.argument(ARG_POSITION, StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                HudPosition.ids(), builder))
                        .executes(ctx -> SettingsCommandHandler.hudPosition(
                                service,
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, ARG_POSITION))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> hudLinesBranch(ReinodoceCommandService service) {
        return Commands.literal("hud-lines")
                .then(Commands.argument(ARG_LINES, IntegerArgumentType.integer(1, ReinodoceConfig.MAX_HUD_LINES))
                        .executes(ctx -> SettingsCommandHandler.hudLines(
                                service,
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, ARG_LINES))));
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
                                        IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("block-word")
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
                        .then(Commands.literal("list")
                                .executes(ctx -> RuleCommandHandler.listBlockedWords(service, ctx.getSource()))))
                .then(Commands.literal("block-user")
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
                        .then(Commands.literal("list")
                                .executes(ctx -> RuleCommandHandler.listBlockedUsers(service, ctx.getSource()))))
                .then(Commands.literal("max-length")
                        .then(Commands.argument("length", IntegerArgumentType.integer(0))
                                .executes(ctx -> RuleCommandHandler.maxLength(
                                        service,
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "length")))))
                .then(Commands.literal("duplicate-cooldown")
                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(0))
                                .executes(ctx -> RuleCommandHandler.duplicateCooldown(
                                        service,
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, ARG_SECONDS)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> syntheticBranch(ReinodoceCommandService service) {
        return Commands.literal("synthetic")
                .then(Commands.literal("gift")
                        .then(Commands.argument(ARG_VALUE, IntegerArgumentType.integer(0))
                                .executes(ctx -> SyntheticCommandHandler.gift(
                                        service,
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, ARG_VALUE)))))
                .then(Commands.literal("gift-combo")
                        .then(Commands.argument("mode", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        GiftComboMode.ids(), builder))
                                .executes(ctx -> SyntheticCommandHandler.giftCombo(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "mode")))))
                .then(syntheticToggle(service, "follow", SyntheticCommandHandler::follow))
                .then(syntheticToggle(service, "join", SyntheticCommandHandler::join))
                .then(syntheticToggle(service, "member-level", SyntheticCommandHandler::memberLevel));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> alertBranch(ReinodoceCommandService service) {
        return Commands.literal("alert")
                .then(alertToggle(service, "gift", AlertEventType.GIFT))
                .then(Commands.literal("gift-min-value")
                        .then(Commands.argument(ARG_VALUE, IntegerArgumentType.integer(0))
                                .executes(ctx -> AlertCommandHandler.giftMinValue(
                                        service,
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, ARG_VALUE)))))
                .then(alertToggle(service, "follow", AlertEventType.FOLLOW))
                .then(alertToggle(service, "join", AlertEventType.JOIN))
                .then(alertToggle(service, "member-level", AlertEventType.MEMBER_LEVEL));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> alertToggle(
            ReinodoceCommandService service, String literal, AlertEventType eventType
    ) {
        return Commands.literal(literal)
                .then(Commands.literal("sound")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> AlertCommandHandler.sound(
                                        service,
                                        ctx.getSource(),
                                        eventType,
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(Commands.literal("toast")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> AlertCommandHandler.toast(
                                        service,
                                        ctx.getSource(),
                                        eventType,
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> loggingBranch(ReinodoceCommandService service) {
        return Commands.literal("logging")
                .then(Commands.literal("enabled")
                        .then(Commands.argument(ARG_ENABLED, BoolArgumentType.bool())
                                .executes(ctx -> LoggingCommandHandler.enabled(
                                        service,
                                        ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, ARG_ENABLED)))))
                .then(Commands.literal(ARG_FORMAT)
                        .then(Commands.argument(ARG_FORMAT, StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        SessionLogFormat.ids(), builder))
                                .executes(ctx -> LoggingCommandHandler.format(
                                        service,
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_FORMAT)))));
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
