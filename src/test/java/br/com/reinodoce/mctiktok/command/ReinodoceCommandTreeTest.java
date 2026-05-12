package br.com.reinodoce.mctiktok.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReinodoceCommandTreeTest {
    private static final String ENABLED_ARGUMENT = "enabled";

    @Test
    void commandTreeExposesDocumentedPublicSurface() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandNode<CommandSourceStack> root = dispatcher.register(ReinodoceCommandTree.build());

        assertNotNull(root.getChild("connect").getChild("username"));
        assertNotNull(root.getChild("disconnect"));
        assertNotNull(root.getChild("status"));
        assertNotNull(root.getChild("reload"));

        CommandNode<CommandSourceStack> settings = root.getChild("settings");
        assertNotNull(settings.getChild("reconnect").getChild("seconds"));
        assertNotNull(settings.getChild("chat-emotes").getChild(ENABLED_ARGUMENT));

        CommandNode<CommandSourceStack> rule = root.getChild("rule");
        assertNotNull(rule.getChild("follower").getChild(ENABLED_ARGUMENT));
        assertNotNull(rule.getChild("min-member-level").getChild("level"));

        CommandNode<CommandSourceStack> syntetic = root.getChild("syntetic");
        assertNotNull(syntetic.getChild("gift").getChild("value"));
        assertNotNull(syntetic.getChild("gift-combo").getChild("mode"));
        assertNotNull(syntetic.getChild("follow").getChild(ENABLED_ARGUMENT));
        assertNotNull(syntetic.getChild("join").getChild(ENABLED_ARGUMENT));
        assertNotNull(syntetic.getChild("member-level").getChild(ENABLED_ARGUMENT));
    }
}
