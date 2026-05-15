package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce diagnostics ...` command execution.
 */
public final class DiagnosticsCommandHandler {
    private DiagnosticsCommandHandler() {
    }

    /**
     * Exports sanitized support diagnostics.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @return Brigadier command result code
     */
    public static int export(ReinodoceCommandService service, CommandSourceStack source) {
        return CommandFeedback.sendResult(source, service.exportDiagnostics());
    }
}
