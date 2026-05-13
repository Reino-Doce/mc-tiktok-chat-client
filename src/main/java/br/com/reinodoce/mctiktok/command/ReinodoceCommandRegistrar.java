package br.com.reinodoce.mctiktok.command;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import java.util.Objects;

/**
 * Forge client-command registration entry point for the `/reinodoce` command tree.
 */
public final class ReinodoceCommandRegistrar {
    private ReinodoceCommandRegistrar() {
    }

    /**
     * Registers the command tree into Forge's client command dispatcher.
     *
     * @param event Forge client-command registration event
     * @param service command service boundary
     */
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event, ReinodoceCommandService service) {
        Objects.requireNonNull(event, "event").getDispatcher()
                .register(ReinodoceCommandTree.build(service));
    }
}
