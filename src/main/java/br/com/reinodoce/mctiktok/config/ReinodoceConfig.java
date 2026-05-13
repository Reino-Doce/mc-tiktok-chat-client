package br.com.reinodoce.mctiktok.config;

import br.com.reinodoce.mctiktok.rules.GiftComboMode;

/**
 * Persisted client configuration for the `/reinodoce` command surface.
 */
@SuppressWarnings("PMD.DataClass")
public class ReinodoceConfig {
    /** Default prefix used before rendered LIVE chat lines. */
    public static final String DEFAULT_CHAT_PREFIX = "[LIVE]";
    private static final int DEFAULT_RECONNECT_SECONDS = 5;
    private static final int DEFAULT_SYNTHETIC_GIFT_MIN_VALUE = 1;

    private String lastUsername = "";
    private int reconnectSeconds = DEFAULT_RECONNECT_SECONDS;
    private boolean ruleFollowerOnly = false;
    private int ruleMinMemberLevel = 0;
    private int synteticGiftMinValue = DEFAULT_SYNTHETIC_GIFT_MIN_VALUE;
    private String synteticGiftComboMode = GiftComboMode.BULK.id();
    private boolean synteticFollowEnabled = false;
    private boolean synteticJoinEnabled = false;
    private boolean synteticMemberLevelEnabled = false;
    private String chatPrefix = DEFAULT_CHAT_PREFIX;
    private boolean chatEmotesEnabled = true;

    /**
     * Creates a configuration instance populated with default values.
     *
     * @return default configuration
     */
    public static ReinodoceConfig defaults() {
        return new ReinodoceConfig();
    }

    /**
     * Creates a sanitized defensive copy.
     *
     * @return copied configuration
     */
    public ReinodoceConfig copy() {
        ReinodoceConfig copy = new ReinodoceConfig();
        copy.setLastUsername(lastUsername);
        copy.setReconnectSeconds(reconnectSeconds);
        copy.setRuleFollowerOnly(ruleFollowerOnly);
        copy.setRuleMinMemberLevel(ruleMinMemberLevel);
        copy.setSynteticGiftMinValue(synteticGiftMinValue);
        copy.setSynteticGiftComboMode(synteticGiftComboMode);
        copy.setSynteticFollowEnabled(synteticFollowEnabled);
        copy.setSynteticJoinEnabled(synteticJoinEnabled);
        copy.setSynteticMemberLevelEnabled(synteticMemberLevelEnabled);
        copy.setChatPrefix(chatPrefix);
        copy.setChatEmotesEnabled(chatEmotesEnabled);
        return copy;
    }

    /**
     * Returns the last successfully connected username.
     *
     * @return last username or blank
     */
    public String getLastUsername() {
        return lastUsername;
    }

    /**
     * Sets the last successfully connected username.
     *
     * @param lastUsername username to persist
     */
    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername == null ? "" : lastUsername.trim();
    }

    /**
     * Returns the reconnect delay in seconds.
     *
     * @return reconnect delay in seconds
     */
    public int getReconnectSeconds() {
        return reconnectSeconds;
    }

    /**
     * Sets the reconnect delay.
     *
     * @param reconnectSeconds reconnect delay in seconds, clamped to zero or greater
     */
    public void setReconnectSeconds(int reconnectSeconds) {
        this.reconnectSeconds = Math.max(0, reconnectSeconds);
    }

    /**
     * Returns whether comments are restricted to followers.
     *
     * @return follower-only rule state
     */
    public boolean isRuleFollowerOnly() {
        return ruleFollowerOnly;
    }

    /**
     * Sets whether comments are restricted to followers.
     *
     * @param ruleFollowerOnly follower-only rule state
     */
    public void setRuleFollowerOnly(boolean ruleFollowerOnly) {
        this.ruleFollowerOnly = ruleFollowerOnly;
    }

    /**
     * Returns the minimum member level required for comments.
     *
     * @return minimum member level
     */
    public int getRuleMinMemberLevel() {
        return ruleMinMemberLevel;
    }

    /**
     * Sets the minimum member level required for comments.
     *
     * @param ruleMinMemberLevel minimum member level, clamped to zero or greater
     */
    public void setRuleMinMemberLevel(int ruleMinMemberLevel) {
        this.ruleMinMemberLevel = Math.max(0, ruleMinMemberLevel);
    }

    /**
     * Returns the minimum gift value for synthetic gift output.
     *
     * @return minimum gift value
     */
    public int getSynteticGiftMinValue() {
        return synteticGiftMinValue;
    }

    /**
     * Sets the minimum gift value for synthetic gift output.
     *
     * @param synteticGiftMinValue minimum gift value, clamped to zero or greater
     */
    public void setSynteticGiftMinValue(int synteticGiftMinValue) {
        this.synteticGiftMinValue = Math.max(0, synteticGiftMinValue);
    }

    /**
     * Returns the synthetic gift combo mode identifier.
     *
     * @return persisted combo mode identifier
     */
    public String getSynteticGiftComboMode() {
        return synteticGiftComboMode;
    }

    /**
     * Sets the synthetic gift combo mode.
     *
     * @param synteticGiftComboMode combo mode identifier
     */
    public void setSynteticGiftComboMode(String synteticGiftComboMode) {
        this.synteticGiftComboMode = GiftComboMode.fromString(synteticGiftComboMode).id();
    }

    /**
     * Returns whether synthetic follow output is enabled.
     *
     * @return synthetic follow state
     */
    public boolean isSynteticFollowEnabled() {
        return synteticFollowEnabled;
    }

    /**
     * Sets whether synthetic follow output is enabled.
     *
     * @param synteticFollowEnabled synthetic follow state
     */
    public void setSynteticFollowEnabled(boolean synteticFollowEnabled) {
        this.synteticFollowEnabled = synteticFollowEnabled;
    }

    /**
     * Returns whether synthetic join output is enabled.
     *
     * @return synthetic join state
     */
    public boolean isSynteticJoinEnabled() {
        return synteticJoinEnabled;
    }

    /**
     * Sets whether synthetic join output is enabled.
     *
     * @param synteticJoinEnabled synthetic join state
     */
    public void setSynteticJoinEnabled(boolean synteticJoinEnabled) {
        this.synteticJoinEnabled = synteticJoinEnabled;
    }

    /**
     * Returns whether synthetic member-level output is enabled.
     *
     * @return synthetic member-level state
     */
    public boolean isSynteticMemberLevelEnabled() {
        return synteticMemberLevelEnabled;
    }

    /**
     * Sets whether synthetic member-level output is enabled.
     *
     * @param synteticMemberLevelEnabled synthetic member-level state
     */
    public void setSynteticMemberLevelEnabled(boolean synteticMemberLevelEnabled) {
        this.synteticMemberLevelEnabled = synteticMemberLevelEnabled;
    }

    /**
     * Returns the chat prefix.
     *
     * @return configured chat prefix
     */
    public String getChatPrefix() {
        return chatPrefix;
    }

    /**
     * Sets the chat prefix, falling back to the default when blank.
     *
     * @param chatPrefix chat prefix to store
     */
    public void setChatPrefix(String chatPrefix) {
        if (chatPrefix == null || chatPrefix.isBlank()) {
            this.chatPrefix = DEFAULT_CHAT_PREFIX;
            return;
        }
        this.chatPrefix = chatPrefix.trim();
    }

    /**
     * Returns whether inline chat emotes are enabled.
     *
     * @return inline chat emote state
     */
    public boolean isChatEmotesEnabled() {
        return chatEmotesEnabled;
    }

    /**
     * Sets whether inline chat emotes are enabled.
     *
     * @param chatEmotesEnabled inline chat emote state
     */
    public void setChatEmotesEnabled(boolean chatEmotesEnabled) {
        this.chatEmotesEnabled = chatEmotesEnabled;
    }
}
