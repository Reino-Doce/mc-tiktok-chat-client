package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.hud.HudMessageStore;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftChatGatewayTest {
    private static final String ALICE = "alice";

    @Test
    void mirroredLiveLinesRespectChatLogSetting() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftChatGateway gateway = gateway(bridge);
        ReinodoceConfig config = ReinodoceConfig.defaults();

        gateway.sendLiveComment(config, ALICE, "hello");

        assertFalse(bridge.lastLogToChat());
    }

    @Test
    void mirroredLiveLinesCanOptBackIntoChatLog() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftChatGateway gateway = gateway(bridge);
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setChatLogEnabled(true);

        gateway.sendSyntheticJoin(config, ALICE);

        assertTrue(bridge.lastLogToChat());
    }

    @Test
    void systemLinesAlwaysUseChatLog() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftChatGateway gateway = gateway(bridge, new HudMessageStore());

        gateway.sendSystem("Connected", true);

        assertTrue(bridge.lastLogToChat());
    }

    @Test
    void actionbarModeRoutesMirroredLinesToActionbar() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftChatGateway gateway = gateway(bridge, new HudMessageStore());
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setOutputMode("actionbar");

        gateway.sendLiveComment(config, ALICE, "hello");

        assertEquals("[LIVE]  <alice> hello", bridge.lastActionbar().getString());
        assertFalse(bridge.chatWritten());
    }

    @Test
    void hudModeStoresMirroredLinesWithoutWritingChat() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        HudMessageStore hudMessageStore = new HudMessageStore();
        MinecraftChatGateway gateway = gateway(bridge, hudMessageStore);
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setOutputMode("hud");
        config.setHudLines(1);

        gateway.sendLiveComment(config, ALICE, "first");
        gateway.sendLiveComment(config, "bob", "second");

        assertEquals(1, hudMessageStore.snapshot(config.getHudLines()).size());
        assertEquals("[LIVE]  <bob> second", hudMessageStore.snapshot(config.getHudLines()).get(0).getString());
        assertFalse(bridge.chatWritten());
    }

    @Test
    void offModeSuppressesMirroredLines() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftChatGateway gateway = gateway(bridge, new HudMessageStore());
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setOutputMode("off");

        gateway.sendLiveComment(config, ALICE, "hello");

        assertFalse(bridge.chatWritten());
        assertEquals("", bridge.lastActionbar().getString());
    }

    private static MinecraftChatGateway gateway(RecordingPlatformBridge bridge) {
        return gateway(bridge, new HudMessageStore());
    }

    private static MinecraftChatGateway gateway(RecordingPlatformBridge bridge, HudMessageStore hudMessageStore) {
        InlineMediaCache mediaCache = new InlineMediaCache();
        return new MinecraftChatGateway(
                bridge,
                new LiveMessageFormatter(new InlineMediaTokenRegistry(mediaCache)),
                mediaCache,
                hudMessageStore);
    }

    private static final class RecordingPlatformBridge implements MinecraftPlatformBridge {
        private boolean recordedLogToChat;
        private boolean recordedChatWrite;
        private Component recordedActionbar = Component.literal("");

        @Override
        public void runOnClientThread(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void addChatMessage(Component component, boolean logToChat) {
            this.recordedLogToChat = logToChat;
            this.recordedChatWrite = true;
        }

        @Override
        public void showActionBarMessage(Component component) {
            this.recordedActionbar = component;
        }

        @Override
        public boolean isClientReady() {
            return true;
        }

        boolean lastLogToChat() {
            return recordedLogToChat;
        }

        boolean chatWritten() {
            return recordedChatWrite;
        }

        Component lastActionbar() {
            return recordedActionbar;
        }
    }
}
