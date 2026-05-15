package br.com.reinodoce.mctiktok.command;

import br.com.reinodoce.mctiktok.alert.AlertEventType;

import java.util.List;

/**
 * Command-facing service contract used by the `/reinodoce` Brigadier tree.
 */
// Public command service intentionally exposes one method per command action.
@SuppressWarnings("PMD.ExcessivePublicCount")
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
     * Writes a sanitized local support diagnostics report.
     *
     * @return command result with the created file path
     */
    CommandResult exportDiagnostics();

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
     * Updates local mirrored output mode.
     *
     * @param mode output mode identifier
     * @return command result
     */
    CommandResult setOutputMode(String mode);

    /**
     * Updates local HUD anchor.
     *
     * @param position HUD position identifier
     * @return command result
     */
    CommandResult setHudPosition(String position);

    /**
     * Updates retained local HUD line count.
     *
     * @param lines HUD line count
     * @return command result
     */
    CommandResult setHudLines(int lines);

    /**
     * Updates local pinned-message overlay visibility.
     *
     * @param enabled whether pinned-message overlay rendering is enabled
     * @return command result
     */
    CommandResult setPinnedOverlayEnabled(boolean enabled);

    /**
     * Updates local pinned-message overlay anchor.
     *
     * @param position pinned-message overlay position identifier
     * @return command result
     */
    CommandResult setPinnedOverlayPosition(String position);

    /**
     * Updates whether pinned messages are also sent through the normal mirrored output mode.
     *
     * @param enabled whether pinned messages should also appear in normal mirrored output
     * @return command result
     */
    CommandResult setPinnedMessagesInOutput(boolean enabled);

    /**
     * Updates retained local pinned-message overlay count.
     *
     * @param messages pinned-message overlay item count
     * @return command result
     */
    CommandResult setPinnedOverlayMessages(int messages);

    /**
     * Updates burst-control state.
     *
     * @param enabled whether burst controls are enabled
     * @return command result
     */
    CommandResult setBurstControlEnabled(boolean enabled);

    /**
     * Updates visible comment throughput for burst control.
     *
     * @param commentsPerSecond visible comments per second, or zero for unlimited
     * @return command result
     */
    CommandResult setBurstCommentsPerSecond(int commentsPerSecond);

    /**
     * Updates join/follow aggregation window for burst control.
     *
     * @param seconds aggregation window seconds, or zero to disable aggregation
     * @return command result
     */
    CommandResult setBurstSyntheticAggregationSeconds(int seconds);

    /**
     * Returns language setting status lines.
     *
     * @return language status lines
     */
    List<String> languageLines();

    /**
     * Updates the language override.
     *
     * @param language {@code auto} or locale
     * @return command result
     */
    CommandResult setLanguage(String language);

    /**
     * Opens the client settings GUI when available.
     *
     * @return command result
     */
    CommandResult openSettingsGui();

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
     * Adds a blocked word filter.
     *
     * @param word blocked word fragment
     * @return command result
     */
    CommandResult addBlockedWord(String word);

    /**
     * Removes a blocked word filter.
     *
     * @param word blocked word fragment
     * @return command result
     */
    CommandResult removeBlockedWord(String word);

    /**
     * Replaces blocked word filters from comma-separated input.
     *
     * @param words comma-separated blocked word fragments
     * @return command result
     */
    CommandResult importBlockedWords(String words);

    /**
     * Returns blocked word filters as export lines.
     *
     * @return blocked word export lines
     */
    List<String> blockedWordExportLines();

    /**
     * Returns blocked word filter lines.
     *
     * @return blocked word lines
     */
    List<String> blockedWordLines();

    /**
     * Adds a blocked user filter.
     *
     * @param username TikTok username
     * @return command result
     */
    CommandResult addBlockedUser(String username);

    /**
     * Removes a blocked user filter.
     *
     * @param username TikTok username
     * @return command result
     */
    CommandResult removeBlockedUser(String username);

    /**
     * Replaces blocked user filters from comma-separated input.
     *
     * @param usernames comma-separated usernames
     * @return command result
     */
    CommandResult importBlockedUsers(String usernames);

    /**
     * Returns blocked user filters as export lines.
     *
     * @return blocked user export lines
     */
    List<String> blockedUserExportLines();

    /**
     * Returns blocked user filter lines.
     *
     * @return blocked user lines
     */
    List<String> blockedUserLines();

    /**
     * Updates emote-only comment filtering.
     *
     * @param enabled whether emote-only comments should be hidden
     * @return command result
     */
    CommandResult setEmoteOnlyFilterRule(boolean enabled);

    /**
     * Updates link/URL comment filtering.
     *
     * @param enabled whether comments containing links should be hidden
     * @return command result
     */
    CommandResult setLinkFilterRule(boolean enabled);

    /**
     * Updates repeated-user cooldown.
     *
     * @param seconds cooldown seconds, or zero to disable
     * @return command result
     */
    CommandResult setUserCooldownRule(int seconds);

    /**
     * Updates user allowlist mode.
     *
     * @param enabled whether allowlist mode is enabled
     * @return command result
     */
    CommandResult setAllowlistModeRule(boolean enabled);

    /**
     * Adds an allowed username.
     *
     * @param username TikTok username
     * @return command result
     */
    CommandResult addAllowedUser(String username);

    /**
     * Removes an allowed username.
     *
     * @param username TikTok username
     * @return command result
     */
    CommandResult removeAllowedUser(String username);

    /**
     * Returns allowed user filter lines.
     *
     * @return allowed user lines
     */
    List<String> allowedUserLines();

    /**
     * Updates maximum accepted message length.
     *
     * @param length maximum length, or zero to disable
     * @return command result
     */
    CommandResult setMaxMessageLengthRule(int length);

    /**
     * Updates duplicate message cooldown.
     *
     * @param seconds cooldown seconds, or zero to disable
     * @return command result
     */
    CommandResult setDuplicateCooldownRule(int seconds);

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
     * Updates local sound alerts for an event type.
     *
     * @param eventType alert event type
     * @param enabled whether sound alerts are enabled
     * @return command result
     */
    CommandResult setAlertSound(AlertEventType eventType, boolean enabled);

    /**
     * Updates the local sound identifier for an event type.
     *
     * @param eventType alert event type
     * @param soundId {@code default} or a Minecraft sound resource id
     * @return command result
     */
    CommandResult setAlertSoundId(AlertEventType eventType, String soundId);

    /**
     * Updates local toast alerts for an event type.
     *
     * @param eventType alert event type
     * @param enabled whether toast alerts are enabled
     * @return command result
     */
    CommandResult setAlertToast(AlertEventType eventType, boolean enabled);

    /**
     * Updates the toast text template for an event type.
     *
     * @param eventType alert event type
     * @param template toast message template, or {@code default} for built-in text
     * @return command result
     */
    CommandResult setAlertToastTemplate(AlertEventType eventType, String template);

    /**
     * Updates the toast media mode for an event type.
     *
     * @param eventType alert event type
     * @param mediaMode media mode identifier
     * @return command result
     */
    CommandResult setAlertMediaMode(AlertEventType eventType, String mediaMode);

    /**
     * Updates the configured custom toast image for an event type.
     *
     * @param eventType alert event type
     * @param customImage image reference, or {@code default} to clear
     * @return command result
     */
    CommandResult setAlertCustomImage(AlertEventType eventType, String customImage);

    /**
     * Updates minimum gift value for local gift alerts.
     *
     * @param value minimum gift value
     * @return command result
     */
    CommandResult setAlertGiftMinValue(int value);

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
     * Updates username masking in mirrored visible output.
     *
     * @param enabled whether mirrored output usernames are masked
     * @return command result
     */
    CommandResult setMaskUsernamesInOutput(boolean enabled);

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
     * Updates local session event logging.
     *
     * @param enabled whether local session logging is enabled
     * @return command result
     */
    CommandResult setSessionLoggingEnabled(boolean enabled);

    /**
     * Updates local session event log format.
     *
     * @param format session log format id
     * @return command result
     */
    CommandResult setSessionLoggingFormat(String format);

    /**
     * Updates session log retention by maximum age.
     *
     * @param days retention days, or zero to disable age cleanup
     * @return command result
     */
    CommandResult setSessionLoggingRetentionDays(int days);

    /**
     * Updates session log retention by maximum file count.
     *
     * @param files retained file count, or zero to disable count cleanup
     * @return command result
     */
    CommandResult setSessionLoggingRetentionFiles(int files);

    /**
     * Updates session log username masking.
     *
     * @param enabled whether session log username fields are masked
     * @return command result
     */
    CommandResult setSessionLoggingAnonymized(boolean enabled);

    /**
     * Updates whether session logs omit message text bodies.
     *
     * @param enabled whether session logs omit message bodies
     * @return command result
     */
    CommandResult setSessionLoggingMetadataOnly(boolean enabled);

    /**
     * Reloads persisted configuration.
     *
     * @return command result
     */
    CommandResult reload();
}
