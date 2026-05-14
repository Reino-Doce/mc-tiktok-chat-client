package br.com.reinodoce.mctiktok.command.handlers;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.command.CommandFeedback;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import net.minecraft.commands.CommandSourceStack;

/**
 * Handles `/reinodoce alert ...` command execution.
 */
public final class AlertCommandHandler {
    private AlertCommandHandler() {
    }

    /**
     * Updates a local alert sound toggle.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param eventType alert event type
     * @param enabled whether sound alerts should be enabled
     * @return Brigadier command result code
     */
    public static int sound(
            ReinodoceCommandService service,
            CommandSourceStack source,
            AlertEventType eventType,
            boolean enabled
    ) {
        return CommandFeedback.sendResult(source, service.setAlertSound(eventType, enabled));
    }

    /**
     * Updates a local alert toast toggle.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param eventType alert event type
     * @param enabled whether toast alerts should be enabled
     * @return Brigadier command result code
     */
    public static int toast(
            ReinodoceCommandService service,
            CommandSourceStack source,
            AlertEventType eventType,
            boolean enabled
    ) {
        return CommandFeedback.sendResult(source, service.setAlertToast(eventType, enabled));
    }

    /**
     * Updates the minimum diamond value for gift alerts.
     *
     * @param service command service boundary
     * @param source command source to receive feedback
     * @param value minimum gift value
     * @return Brigadier command result code
     */
    public static int giftMinValue(ReinodoceCommandService service, CommandSourceStack source, int value) {
        return CommandFeedback.sendResult(source, service.setAlertGiftMinValue(value));
    }
}
