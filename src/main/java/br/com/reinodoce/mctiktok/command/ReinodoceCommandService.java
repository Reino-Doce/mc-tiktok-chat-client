package br.com.reinodoce.mctiktok.command;

import java.util.List;

/**
 * Command-facing service contract used by the `/reinodoce` Brigadier tree.
 */
public interface ReinodoceCommandService {
    /**
     * Starts a TikTok LIVE connection.
     *
     * @param username TikTok username argument
     * @return command result
     */
    CommandResult connect(String username);

    /**
     * Starts a TikTok LIVE connection using the saved last username.
     *
     * @return command result
     */
    CommandResult connectLast();

    /**
     * Stops the active TikTok LIVE connection.
     *
     * @return command result
     */
    CommandResult disconnect();

    /**
     * Returns status lines for `/reinodoce status`.
     *
     * @return ordered status lines
     */
    List<String> statusLines();

    /**
     * Returns current session stats lines for `/reinodoce stats`.
     *
     * @return ordered stats lines
     */
    List<String> statsLines();

    /**
     * Resets current session stats.
     *
     * @return command result
     */
    CommandResult resetStats();

    /**
     * Updates reconnect delay.
     *
     * @param seconds reconnect delay in seconds
     * @return command result
     */
    CommandResult setReconnectSeconds(int seconds);

    /**
     * Updates startup auto-connect.
     *
     * @param enabled whether the client should connect to the saved username on startup
     * @return command result
     */
    CommandResult setAutoConnectOnStart(boolean enabled);

    /**
     * Updates follower-only comment filtering.
     *
     * @param enabled whether follower-only mode is enabled
     * @return command result
     */
    CommandResult setFollowerRule(boolean enabled);

    /**
     * Updates minimum member-level comment filtering.
     *
     * @param level minimum allowed member level
     * @return command result
     */
    CommandResult setMinMemberLevelRule(int level);

    /**
     * Updates minimum gift value for synthetic gift output.
     *
     * @param value minimum gift value
     * @return command result
     */
    CommandResult setSyntheticGift(int value);

    /**
     * Updates synthetic gift combo aggregation mode.
     *
     * @param mode combo mode identifier
     * @return command result
     */
    CommandResult setSyntheticGiftComboMode(String mode);

    /**
     * Updates synthetic follow output.
     *
     * @param enabled whether synthetic follow output is enabled
     * @return command result
     */
    CommandResult setSyntheticFollow(boolean enabled);

    /**
     * Updates synthetic join output.
     *
     * @param enabled whether synthetic join output is enabled
     * @return command result
     */
    CommandResult setSyntheticJoin(boolean enabled);

    /**
     * Updates synthetic member-level output.
     *
     * @param enabled whether synthetic member-level output is enabled
     * @return command result
     */
    CommandResult setSyntheticMemberLevel(boolean enabled);

    /**
     * Updates inline emote rendering.
     *
     * @param enabled whether inline emotes are enabled
     * @return command result
     */
    CommandResult setChatEmotesEnabled(boolean enabled);

    /**
     * Updates mirrored chat logging.
     *
     * @param enabled whether mirrored TikTok lines are written to Minecraft logs
     * @return command result
     */
    CommandResult setChatLogEnabled(boolean enabled);

    /**
     * Updates the visible LIVE chat prefix.
     *
     * @param prefix prefix text
     * @return command result
     */
    CommandResult setChatPrefix(String prefix);

    /**
     * Updates the LIVE chat message format template.
     *
     * @param format format template
     * @return command result
     */
    CommandResult setChatFormat(String format);

    /**
     * Reloads persisted configuration.
     *
     * @return command result
     */
    CommandResult reload();
}
