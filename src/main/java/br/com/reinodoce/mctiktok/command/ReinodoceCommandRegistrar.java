package br.com.reinodoce.mctiktok.command;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class ReinodoceCommandRegistrar {
    private ReinodoceCommandRegistrar() {
    }

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(ReinodoceCommandTree.build());
    }
}
