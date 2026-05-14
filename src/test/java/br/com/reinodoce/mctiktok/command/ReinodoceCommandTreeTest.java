package br.com.reinodoce.mctiktok.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReinodoceCommandTreeTest {
    private static final String ENABLED_ARGUMENT = "enabled";

    @Test
    void commandTreeExposesDocumentedPublicSurface() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandNode<CommandSourceStack> root = dispatcher.register(ReinodoceCommandTree.build(new StubCommandService()));

        CommandNode<CommandSourceStack> connect = root.getChild("connect");
        assertNotNull(connect.getCommand());
        assertNotNull(connect.getChild("username"));
        assertNotNull(root.getChild("disconnect"));
        assertNotNull(root.getChild("status"));
        assertNotNull(root.getChild("stats").getChild("reset"));
        assertNotNull(root.getChild("reload"));

        CommandNode<CommandSourceStack> settings = root.getChild("settings");
        assertNotNull(settings.getChild("reconnect").getChild("seconds"));
        assertNotNull(settings.getChild("auto-connect").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("chat-emotes").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("chat-log").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("prefix").getChild("value"));
        assertNotNull(settings.getChild("format").getChild("template"));

        CommandNode<CommandSourceStack> rule = root.getChild("rule");
        assertNotNull(rule.getChild("follower").getChild(ENABLED_ARGUMENT));
        assertNotNull(rule.getChild("min-member-level").getChild("level"));

        CommandNode<CommandSourceStack> synthetic = root.getChild("synthetic");
        assertNotNull(synthetic.getChild("gift").getChild("value"));
        assertNotNull(synthetic.getChild("gift-combo").getChild("mode"));
        assertNotNull(synthetic.getChild("follow").getChild(ENABLED_ARGUMENT));
        assertNotNull(synthetic.getChild("join").getChild(ENABLED_ARGUMENT));
        assertNotNull(synthetic.getChild("member-level").getChild(ENABLED_ARGUMENT));
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
        public CommandResult setFollowerRule(boolean enabled) {
            return unsupported();
        }

        @Override
        public CommandResult setMinMemberLevelRule(int level) {
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
        public CommandResult reload() {
            return unsupported();
        }

        private CommandResult unsupported() {
            throw new UnsupportedOperationException("Command execution is outside this structure test.");
        }
    }
}
