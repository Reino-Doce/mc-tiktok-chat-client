package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftChatGatewayTest {
    @Test
    void mirroredLiveLinesRespectChatLogSetting() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftChatGateway gateway = gateway(bridge);
        ReinodoceConfig config = ReinodoceConfig.defaults();

        gateway.sendLiveComment(config, "alice", "hello");

        assertFalse(bridge.lastLogToChat());
    }

    @Test
    void mirroredLiveLinesCanOptBackIntoChatLog() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftChatGateway gateway = gateway(bridge);
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setChatLogEnabled(true);

        gateway.sendSyntheticJoin(config, "alice");

        assertTrue(bridge.lastLogToChat());
    }

    @Test
    void systemLinesAlwaysUseChatLog() {
        RecordingPlatformBridge bridge = new RecordingPlatformBridge();
        MinecraftChatGateway gateway = gateway(bridge);

        gateway.sendSystem("Connected", true);

        assertTrue(bridge.lastLogToChat());
    }

    private static MinecraftChatGateway gateway(RecordingPlatformBridge bridge) {
        InlineMediaCache mediaCache = new InlineMediaCache();
        return new MinecraftChatGateway(
                bridge,
                new LiveMessageFormatter(new InlineMediaTokenRegistry(mediaCache)),
                mediaCache);
    }

    private static final class RecordingPlatformBridge implements MinecraftPlatformBridge {
        private boolean recordedLogToChat;

        @Override
        public void runOnClientThread(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void addChatMessage(Component component, boolean logToChat) {
            this.recordedLogToChat = logToChat;
        }

        @Override
        public boolean isClientReady() {
            return true;
        }

        boolean lastLogToChat() {
            return recordedLogToChat;
        }
    }
}
