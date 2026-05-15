package br.com.reinodoce.mctiktok.client.pinned;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.pinned.PinnedLiveMessage;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftPinnedMessageGatewayTest {
    @Test
    void usernameMaskingAppliesToPinnedOverlay() {
        PinnedMessageStore store = new PinnedMessageStore();
        MinecraftPinnedMessageGateway gateway = new MinecraftPinnedMessageGateway(new RecordingPlatformBridge(), store);
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setMaskUsernamesInOutput(true);

        gateway.showPinnedMessage(config, new PinnedLiveMessage(1L, "alice", "message", Duration.ofSeconds(10)));

        assertEquals("reinodoce.pinned.label @viewer: message", store.snapshot(config.getPinnedOverlayMessages())
                .get(0).component().getString());
    }

    private static final class RecordingPlatformBridge implements MinecraftPlatformBridge {
        @Override
        public void runOnClientThread(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void addChatMessage(Component component, boolean logToChat) {
        }

        @Override
        public boolean isClientReady() {
            return true;
        }
    }
}
