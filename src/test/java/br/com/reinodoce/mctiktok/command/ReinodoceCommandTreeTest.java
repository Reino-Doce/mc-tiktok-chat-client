package br.com.reinodoce.mctiktok.command;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReinodoceCommandTreeTest {
    private static final String ENABLED_ARGUMENT = "enabled";
    private static final String SECONDS_ARGUMENT = "seconds";
    private static final String USERNAME_ARGUMENT = "username";
    private static final String VALUE_ARGUMENT = "value";
    private static final String SOUND_LITERAL = "sound";
    private static final String TOAST_LITERAL = "toast";
    private static final String MODE_ARGUMENT = "mode";
    private static final String POSITION_ARGUMENT = "position";
    private static final String LINES_ARGUMENT = "lines";

    @Test
    void commandTreeExposesDocumentedPublicSurface() {
        CommandNode<CommandSourceStack> root = root();

        CommandNode<CommandSourceStack> connect = root.getChild("connect");
        assertNotNull(connect.getCommand());
        assertNotNull(connect.getChild(USERNAME_ARGUMENT));
        assertNotNull(root.getChild("disconnect"));
        assertNotNull(root.getChild("status"));
        assertNotNull(root.getChild("stats").getChild("reset"));
        assertNotNull(root.getChild("reload"));

        CommandNode<CommandSourceStack> settings = root.getChild("settings");
        assertNotNull(settings.getChild("reconnect").getChild(SECONDS_ARGUMENT));
        assertNotNull(settings.getChild("auto-connect").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("output").getChild(MODE_ARGUMENT));
        assertNotNull(settings.getChild("hud-position").getChild(POSITION_ARGUMENT));
        assertNotNull(settings.getChild("hud-lines").getChild(LINES_ARGUMENT));
        assertNotNull(settings.getChild("chat-emotes").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("chat-log").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("prefix").getChild(VALUE_ARGUMENT));
        assertNotNull(settings.getChild("format").getChild("template"));

        CommandNode<CommandSourceStack> rule = root.getChild("rule");
        assertNotNull(rule.getChild("follower").getChild(ENABLED_ARGUMENT));
        assertNotNull(rule.getChild("min-member-level").getChild("level"));
        assertNotNull(rule.getChild("block-word").getChild("add").getChild(VALUE_ARGUMENT));
        assertNotNull(rule.getChild("block-word").getChild("remove").getChild(VALUE_ARGUMENT));
        assertNotNull(rule.getChild("block-word").getChild("list"));
        assertNotNull(rule.getChild("block-user").getChild("add").getChild(USERNAME_ARGUMENT));
        assertNotNull(rule.getChild("block-user").getChild("remove").getChild(USERNAME_ARGUMENT));
        assertNotNull(rule.getChild("block-user").getChild("list"));
        assertNotNull(rule.getChild("max-length").getChild("length"));
        assertNotNull(rule.getChild("duplicate-cooldown").getChild(SECONDS_ARGUMENT));

        CommandNode<CommandSourceStack> synthetic = root.getChild("synthetic");
        assertNotNull(synthetic.getChild("gift").getChild(VALUE_ARGUMENT));
        assertNotNull(synthetic.getChild("gift-combo").getChild("mode"));
        assertNotNull(synthetic.getChild("follow").getChild(ENABLED_ARGUMENT));
        assertNotNull(synthetic.getChild("join").getChild(ENABLED_ARGUMENT));
        assertNotNull(synthetic.getChild("member-level").getChild(ENABLED_ARGUMENT));

        CommandNode<CommandSourceStack> logging = root.getChild("logging");
        assertNotNull(logging.getChild("enabled").getChild(ENABLED_ARGUMENT));
        assertNotNull(logging.getChild("format").getChild("format"));
    }

    @Test
    void commandTreeExposesAlertSurface() {
        CommandNode<CommandSourceStack> alert = root().getChild("alert");

        assertNotNull(alert.getChild("gift").getChild(SOUND_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild("gift").getChild(TOAST_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild("gift-min-value").getChild(VALUE_ARGUMENT));
        assertNotNull(alert.getChild("follow").getChild(SOUND_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild("follow").getChild(TOAST_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild("join").getChild(SOUND_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild("join").getChild(TOAST_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild("member-level").getChild(SOUND_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild("member-level").getChild(TOAST_LITERAL).getChild(ENABLED_ARGUMENT));
    }

    private static CommandNode<CommandSourceStack> root() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        return dispatcher.register(ReinodoceCommandTree.build(new StubCommandService()));
    }

    private static final class StubCommandService implements ReinodoceCommandService {
        @Override
        public CommandResult connect(String username) {
            return unsupported();
        }

        @Override
        public CommandResult connectLast() {
            return unsupported();
        }

        @Override
        public CommandResult disconnect() {
            return unsupported();
        }

        @Override
        public List<String> statusLines() {
            return List.of();
        }

        @Override
        public List<String> statsLines() {
            return List.of();
        }

        @Override
        public CommandResult resetStats() {
            return unsupported();
        }

        @Override
        public CommandResult setReconnectSeconds(int seconds) {
            return unsupported();
        }

        @Override
        public CommandResult setAutoConnectOnStart(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setOutputMode(String mode) {
            return unsupported();
        }

        @Override
        public CommandResult setHudPosition(String position) {
            return unsupported();
        }

        @Override
        public CommandResult setHudLines(int lines) {
            return unsupported();
        }

        @Override
        public CommandResult setFollowerRule(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setMinMemberLevelRule(int level) {
            return unsupported();
        }

        @Override
        public CommandResult addBlockedWord(String word) {
            return unsupported();
        }

        @Override
        public CommandResult removeBlockedWord(String word) {
            return unsupported();
        }

        @Override
        public List<String> blockedWordLines() {
            return List.of();
        }

        @Override
        public CommandResult addBlockedUser(String username) {
            return unsupported();
        }

        @Override
        public CommandResult removeBlockedUser(String username) {
            return unsupported();
        }

        @Override
        public List<String> blockedUserLines() {
            return List.of();
        }

        @Override
        public CommandResult setMaxMessageLengthRule(int length) {
            return unsupported();
        }

        @Override
        public CommandResult setDuplicateCooldownRule(int seconds) {
            return unsupported();
        }

        @Override
        public CommandResult setSyntheticGift(int value) {
            return unsupported();
        }

        @Override
        public CommandResult setSyntheticGiftComboMode(String mode) {
            return unsupported();
        }

        @Override
        public CommandResult setSyntheticFollow(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setSyntheticJoin(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setSyntheticMemberLevel(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setAlertSound(AlertEventType eventType, boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setAlertToast(AlertEventType eventType, boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setAlertGiftMinValue(int value) {
            return unsupported();
        }

        @Override
        public CommandResult setChatEmotesEnabled(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setChatLogEnabled(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setChatPrefix(String prefix) {
            return unsupported();
        }

        @Override
        public CommandResult setChatFormat(String format) {
            return unsupported();
        }

        @Override
        public CommandResult setSessionLoggingEnabled(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setSessionLoggingFormat(String format) {
            return unsupported();
        }

        @Override
        public CommandResult reload() {
            return unsupported();
        }

        private CommandResult unsupported() {
            throw new UnsupportedOperationException("Command execution is outside this structure test.");
        }
    }
}
