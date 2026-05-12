package br.com.reinodoce.mctiktok.command;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import java.util.Objects;

public final class ReinodoceCommandRegistrar {
    private ReinodoceCommandRegistrar() {
    }

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event, ReinodoceCommandService service) {
        Objects.requireNonNull(event, "event").getDispatcher()
                .register(ReinodoceCommandTree.build(service));
    }
}
