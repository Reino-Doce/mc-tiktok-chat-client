package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.command.ReinodoceCommandRegistrar;
import br.com.reinodoce.mctiktok.platform.mc1201.Forge1201PlatformBridge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class ReinodoceClientBootstrap {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final ReinodoceClientService SERVICE = new ReinodoceClientService(new Forge1201PlatformBridge());

    private ReinodoceClientBootstrap() {
    }

    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        SERVICE.initialize();
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                ReinodoceCommandRegistrar.onRegisterClientCommands(event, SERVICE));
    }

    public static ReinodoceClientService service() {
        return SERVICE;
    }
}
