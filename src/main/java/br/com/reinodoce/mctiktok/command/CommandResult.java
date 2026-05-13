package br.com.reinodoce.mctiktok.command;

/**
 * Command-layer outcome with the text that should be shown to the player.
 *
 * @param success whether the command completed successfully
 * @param message feedback text to show
 */
public record CommandResult(boolean success, String message) {
    /**
     * Creates a successful command result.
     *
     * @param message feedback text to show
     * @return successful result
     */
    public static CommandResult ok(String message) {
        return new CommandResult(true, message);
    }

    /**
     * Creates a failed command result.
     *
     * @param message feedback text to show
     * @return failed result
     */
    public static CommandResult error(String message) {
        return new CommandResult(false, message);
    }
}
