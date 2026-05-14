package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.command.ReinodoceCommandRegistrar;
import br.com.reinodoce.mctiktok.client.gui.ReinodoceSettingsScreen;
import br.com.reinodoce.mctiktok.platform.mc1201.Forge1201PlatformBridge;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-side bootstrap that wires services and registers Forge client commands once.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class ReinodoceClientBootstrap {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final ReinodoceClientService SERVICE = new ReinodoceClientService(new Forge1201PlatformBridge());

    private ReinodoceClientBootstrap() {
    }

    /**
     * Initializes the singleton client service and command registration hook.
     */
    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        SERVICE.initialize();
        FMLJavaModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        new ReinodoceSettingsScreen(SERVICE, SERVICE.currentConfig(), parent)));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (RegisterGuiOverlaysEvent event) -> event.registerAboveAll(
                        "reinodoce_live_output",
                        (gui, graphics, partialTick, screenWidth, screenHeight) ->
                                SERVICE.renderHud(graphics, screenWidth, screenHeight)));
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                ReinodoceCommandRegistrar.onRegisterClientCommands(event, SERVICE));
    }

    /**
     * Returns the singleton client service.
     *
     * @return client service singleton
     */
    public static ReinodoceClientService service() {
        return SERVICE;
    }
}
