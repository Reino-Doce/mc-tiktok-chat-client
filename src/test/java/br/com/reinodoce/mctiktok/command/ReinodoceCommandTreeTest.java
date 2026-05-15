package br.com.reinodoce.mctiktok.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReinodoceCommandTreeTest {
    private static final String ENABLED_ARGUMENT = "enabled";
    private static final String SECONDS_ARGUMENT = "seconds";
    private static final String USERNAME_ARGUMENT = "username";
    private static final String VALUE_ARGUMENT = "value";
    private static final String SOUND_ID_ARGUMENT = "soundId";
    private static final String SOUND_LITERAL = "sound";
    private static final String SOUND_ID_LITERAL = "sound-id";
    private static final String TOAST_LITERAL = "toast";
    private static final String TEMPLATE_LITERAL = "template";
    private static final String MEDIA_MODE_LITERAL = "media-mode";
    private static final String CUSTOM_IMAGE_LITERAL = "custom-image";
    private static final String GIFT_LITERAL = "gift";
    private static final String FOLLOW_LITERAL = "follow";
    private static final String JOIN_LITERAL = "join";
    private static final String MEMBER_LEVEL_LITERAL = "member-level";
    private static final String BLOCK_WORD_LITERAL = "block-word";
    private static final String BLOCK_USER_LITERAL = "block-user";
    private static final String ALLOWLIST_LITERAL = "allowlist";
    private static final String MODE_ARGUMENT = "mode";
    private static final String POSITION_ARGUMENT = "position";
    private static final String LINES_ARGUMENT = "lines";
    private static final String MESSAGES_ARGUMENT = "messages";
    private static final String COUNT_ARGUMENT = "count";
    private static final String DAYS_ARGUMENT = "days";
    private static final String FILES_ARGUMENT = "files";
    private static final String LOCALE_ARGUMENT = "locale";
    private static final String CUSTOM_IMAGE_ARGUMENT = "customImage";

    @Test
    void commandTreeExposesDocumentedPublicSurface() {
        CommandNode<CommandSourceStack> root = root();

        CommandNode<CommandSourceStack> connect = root.getChild("connect");
        assertNotNull(connect.getCommand());
        assertNotNull(connect.getChild(USERNAME_ARGUMENT));
        assertNotNull(root.getChild("disconnect"));
        assertNotNull(root.getChild("status"));
        assertNotNull(root.getChild("stats").getChild("reset"));
        assertNotNull(root.getChild("diagnostics").getChild("export"));
        assertNotNull(root.getChild("reload"));

        assertSettingsSurface(root.getChild("settings"));

        assertRuleSurface(root.getChild("rule"));

        CommandNode<CommandSourceStack> synthetic = root.getChild("synthetic");
        assertNotNull(synthetic.getChild(GIFT_LITERAL).getChild(VALUE_ARGUMENT));
        assertNotNull(synthetic.getChild("gift-combo").getChild("mode"));
        assertNotNull(synthetic.getChild(FOLLOW_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(synthetic.getChild(JOIN_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(synthetic.getChild(MEMBER_LEVEL_LITERAL).getChild(ENABLED_ARGUMENT));

        CommandNode<CommandSourceStack> logging = root.getChild("logging");
        assertNotNull(logging.getChild(ENABLED_ARGUMENT).getChild(ENABLED_ARGUMENT));
        assertNotNull(logging.getChild("format").getChild("format"));
        assertNotNull(logging.getChild("retention-days").getChild(DAYS_ARGUMENT));
        assertNotNull(logging.getChild("retention-files").getChild(FILES_ARGUMENT));
        assertNotNull(logging.getChild("anonymized").getChild(ENABLED_ARGUMENT));
        assertNotNull(logging.getChild("metadata-only").getChild(ENABLED_ARGUMENT));
    }

    @Test
    void commandTreeExposesAlertSurface() {
        CommandNode<CommandSourceStack> alert = root().getChild("alert");

        assertNotNull(alert.getChild(GIFT_LITERAL).getChild(SOUND_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild(GIFT_LITERAL).getChild(SOUND_ID_LITERAL).getChild(SOUND_ID_ARGUMENT));
        assertNotNull(alert.getChild(GIFT_LITERAL).getChild(TOAST_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild(GIFT_LITERAL).getChild(TEMPLATE_LITERAL).getChild(TEMPLATE_LITERAL));
        assertNotNull(alert.getChild(GIFT_LITERAL).getChild(MEDIA_MODE_LITERAL).getChild(MODE_ARGUMENT));
        assertNotNull(alert.getChild(GIFT_LITERAL).getChild(CUSTOM_IMAGE_LITERAL).getChild(CUSTOM_IMAGE_ARGUMENT));
        assertNotNull(alert.getChild("gift-min-value").getChild(VALUE_ARGUMENT));
        assertNotNull(alert.getChild(FOLLOW_LITERAL).getChild(SOUND_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild(FOLLOW_LITERAL).getChild(SOUND_ID_LITERAL).getChild(SOUND_ID_ARGUMENT));
        assertNotNull(alert.getChild(FOLLOW_LITERAL).getChild(TOAST_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild(FOLLOW_LITERAL).getChild(TEMPLATE_LITERAL).getChild(TEMPLATE_LITERAL));
        assertNotNull(alert.getChild(FOLLOW_LITERAL).getChild(MEDIA_MODE_LITERAL).getChild(MODE_ARGUMENT));
        assertNotNull(alert.getChild(FOLLOW_LITERAL).getChild(CUSTOM_IMAGE_LITERAL).getChild(CUSTOM_IMAGE_ARGUMENT));
        assertNotNull(alert.getChild(JOIN_LITERAL).getChild(SOUND_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild(JOIN_LITERAL).getChild(SOUND_ID_LITERAL).getChild(SOUND_ID_ARGUMENT));
        assertNotNull(alert.getChild(JOIN_LITERAL).getChild(TOAST_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild(JOIN_LITERAL).getChild(TEMPLATE_LITERAL).getChild(TEMPLATE_LITERAL));
        assertNotNull(alert.getChild(JOIN_LITERAL).getChild(MEDIA_MODE_LITERAL).getChild(MODE_ARGUMENT));
        assertNotNull(alert.getChild(JOIN_LITERAL).getChild(CUSTOM_IMAGE_LITERAL).getChild(CUSTOM_IMAGE_ARGUMENT));
        assertNotNull(alert.getChild(MEMBER_LEVEL_LITERAL).getChild(SOUND_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild(MEMBER_LEVEL_LITERAL).getChild(SOUND_ID_LITERAL).getChild(SOUND_ID_ARGUMENT));
        assertNotNull(alert.getChild(MEMBER_LEVEL_LITERAL).getChild(TOAST_LITERAL).getChild(ENABLED_ARGUMENT));
        assertNotNull(alert.getChild(MEMBER_LEVEL_LITERAL).getChild(TEMPLATE_LITERAL).getChild(TEMPLATE_LITERAL));
        assertNotNull(alert.getChild(MEMBER_LEVEL_LITERAL).getChild(MEDIA_MODE_LITERAL).getChild(MODE_ARGUMENT));
        assertNotNull(alert.getChild(MEMBER_LEVEL_LITERAL).getChild(CUSTOM_IMAGE_LITERAL)
                .getChild(CUSTOM_IMAGE_ARGUMENT));
    }

    private static CommandNode<CommandSourceStack> root() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        return dispatcher.register(ReinodoceCommandTree.build(stubService()));
    }

    private static ReinodoceCommandService stubService() {
        return (ReinodoceCommandService) Proxy.newProxyInstance(
                ReinodoceCommandService.class.getClassLoader(),
                new Class<?>[] {ReinodoceCommandService.class},
                (proxy, method, args) -> defaultReturn(method.getReturnType()));
    }

    private static Object defaultReturn(Class<?> returnType) {
        if (returnType == List.class) {
            return List.of();
        }
        if (returnType == CommandResult.class) {
            return CommandResult.error("unused");
        }
        throw new UnsupportedOperationException("Unsupported command service return type: " + returnType);
    }

    private static void assertSettingsSurface(CommandNode<CommandSourceStack> settings) {
        assertNotNull(settings);
        assertNotNull(settings.getChild("reconnect").getChild(SECONDS_ARGUMENT));
        assertNotNull(settings.getChild("auto-connect").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("language").getCommand());
        assertNotNull(settings.getChild("language").getChild(LOCALE_ARGUMENT));
        assertNotNull(settings.getChild("output").getChild(MODE_ARGUMENT));
        assertNotNull(settings.getChild("hud-position").getChild(POSITION_ARGUMENT));
        assertNotNull(settings.getChild("hud-lines").getChild(LINES_ARGUMENT));
        CommandNode<CommandSourceStack> pinnedOverlay = settings.getChild("pinned-overlay");
        assertNotNull(pinnedOverlay.getChild(ENABLED_ARGUMENT).getChild(ENABLED_ARGUMENT));
        assertNotNull(pinnedOverlay.getChild("position").getChild(POSITION_ARGUMENT));
        assertNotNull(pinnedOverlay.getChild("mirror-output").getChild(ENABLED_ARGUMENT));
        assertNotNull(pinnedOverlay.getChild("messages").getChild(MESSAGES_ARGUMENT));
        CommandNode<CommandSourceStack> burst = settings.getChild("burst");
        assertNotNull(burst.getChild(ENABLED_ARGUMENT).getChild(ENABLED_ARGUMENT));
        assertNotNull(burst.getChild("comments-per-second").getChild(COUNT_ARGUMENT));
        assertNotNull(burst.getChild("synthetic-window").getChild(SECONDS_ARGUMENT));
        assertNotNull(settings.getChild("gui").getCommand());
        assertNotNull(settings.getChild("chat-emotes").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("chat-log").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("mask-usernames").getChild(ENABLED_ARGUMENT));
        assertNotNull(settings.getChild("prefix").getChild(VALUE_ARGUMENT));
        assertNotNull(settings.getChild("format").getChild("template"));
    }

    private static void assertRuleSurface(CommandNode<CommandSourceStack> rule) {
        assertNotNull(rule.getChild("follower").getChild(ENABLED_ARGUMENT));
        assertNotNull(rule.getChild("min-member-level").getChild("level"));
        assertNotNull(rule.getChild(BLOCK_WORD_LITERAL).getChild("add").getChild(VALUE_ARGUMENT));
        assertNotNull(rule.getChild(BLOCK_WORD_LITERAL).getChild("remove").getChild(VALUE_ARGUMENT));
        assertNotNull(rule.getChild(BLOCK_WORD_LITERAL).getChild("import").getChild(VALUE_ARGUMENT));
        assertNotNull(rule.getChild(BLOCK_WORD_LITERAL).getChild("export"));
        assertNotNull(rule.getChild(BLOCK_WORD_LITERAL).getChild("list"));
        assertNotNull(rule.getChild(BLOCK_USER_LITERAL).getChild("add").getChild(USERNAME_ARGUMENT));
        assertNotNull(rule.getChild(BLOCK_USER_LITERAL).getChild("remove").getChild(USERNAME_ARGUMENT));
        assertNotNull(rule.getChild(BLOCK_USER_LITERAL).getChild("import").getChild(VALUE_ARGUMENT));
        assertNotNull(rule.getChild(BLOCK_USER_LITERAL).getChild("export"));
        assertNotNull(rule.getChild(BLOCK_USER_LITERAL).getChild("list"));
        assertNotNull(rule.getChild("emote-only").getChild(ENABLED_ARGUMENT));
        assertNotNull(rule.getChild("links").getChild(ENABLED_ARGUMENT));
        assertNotNull(rule.getChild("user-cooldown").getChild(SECONDS_ARGUMENT));
        assertNotNull(rule.getChild(ALLOWLIST_LITERAL).getChild(ENABLED_ARGUMENT).getChild(ENABLED_ARGUMENT));
        assertNotNull(rule.getChild(ALLOWLIST_LITERAL).getChild("add").getChild(USERNAME_ARGUMENT));
        assertNotNull(rule.getChild(ALLOWLIST_LITERAL).getChild("remove").getChild(USERNAME_ARGUMENT));
        assertNotNull(rule.getChild(ALLOWLIST_LITERAL).getChild("list"));
        assertNotNull(rule.getChild("max-length").getChild("length"));
        assertNotNull(rule.getChild("duplicate-cooldown").getChild(SECONDS_ARGUMENT));
    }
}
