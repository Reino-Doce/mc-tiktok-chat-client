package br.com.reinodoce.mctiktok.config;

import br.com.reinodoce.mctiktok.rules.GiftComboMode;

/**
 * Persisted client configuration for the `/reinodoce` command surface.
 */
// Legacy dual-write fields keep downgraded clients compatible.
@SuppressWarnings({"PMD.DataClass", "PMD.TooManyFields"})
public class ReinodoceConfig {
    /** Default prefix used before rendered LIVE chat lines. */
    public static final String DEFAULT_CHAT_PREFIX = "[LIVE]";
    /** Default template preserving the existing LIVE chat line layout. */
    public static final String DEFAULT_CHAT_FORMAT = "{prefix}  <{username}> {message}";
    private static final int DEFAULT_RECONNECT_SECONDS = 5;
    private static final int DEFAULT_SYNTHETIC_GIFT_MIN_VALUE = 1;

    private String lastUsername = "";
    private int reconnectSeconds = DEFAULT_RECONNECT_SECONDS;
    private boolean ruleFollowerOnly = false;
    private int ruleMinMemberLevel = 0;
    private Integer syntheticGiftMinValue;
    private String syntheticGiftComboMode;
    private Boolean syntheticFollowEnabled;
    private Boolean syntheticJoinEnabled;
    private Boolean syntheticMemberLevelEnabled;
    @Deprecated
    private Integer synteticGiftMinValue;
    @Deprecated
    private String synteticGiftComboMode;
    @Deprecated
    private Boolean synteticFollowEnabled;
    @Deprecated
    private Boolean synteticJoinEnabled;
    @Deprecated
    private Boolean synteticMemberLevelEnabled;
    private String chatPrefix = DEFAULT_CHAT_PREFIX;
    private String chatFormat = DEFAULT_CHAT_FORMAT;
    private boolean chatEmotesEnabled = true;
    private boolean chatLogEnabled = false;

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
        copy.setSyntheticGiftMinValue(getSyntheticGiftMinValue());
        copy.setSyntheticGiftComboMode(getSyntheticGiftComboMode());
        copy.setSyntheticFollowEnabled(isSyntheticFollowEnabled());
        copy.setSyntheticJoinEnabled(isSyntheticJoinEnabled());
        copy.setSyntheticMemberLevelEnabled(isSyntheticMemberLevelEnabled());
        copy.setChatPrefix(chatPrefix);
        copy.setChatFormat(chatFormat);
        copy.setChatEmotesEnabled(chatEmotesEnabled);
        copy.setChatLogEnabled(chatLogEnabled);
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
    public int getSyntheticGiftMinValue() {
        Integer value = syntheticGiftMinValue == null ? synteticGiftMinValue : syntheticGiftMinValue;
        return value == null ? DEFAULT_SYNTHETIC_GIFT_MIN_VALUE : Math.max(0, value);
    }

    /**
     * Sets the minimum gift value for synthetic gift output.
     *
     * @param syntheticGiftMinValue minimum gift value, clamped to zero or greater
     */
    public void setSyntheticGiftMinValue(int syntheticGiftMinValue) {
        int normalized = Math.max(0, syntheticGiftMinValue);
        this.syntheticGiftMinValue = normalized;
        this.synteticGiftMinValue = normalized;
    }

    /**
     * Returns the synthetic gift combo mode identifier.
     *
     * @return persisted combo mode identifier
     */
    public String getSyntheticGiftComboMode() {
        String value = syntheticGiftComboMode == null ? synteticGiftComboMode : syntheticGiftComboMode;
        return GiftComboMode.fromString(value).id();
    }

    /**
     * Sets the synthetic gift combo mode.
     *
     * @param syntheticGiftComboMode combo mode identifier
     */
    public void setSyntheticGiftComboMode(String syntheticGiftComboMode) {
        String normalized = GiftComboMode.fromString(syntheticGiftComboMode).id();
        this.syntheticGiftComboMode = normalized;
        this.synteticGiftComboMode = normalized;
    }

    /**
     * Returns whether synthetic follow output is enabled.
     *
     * @return synthetic follow state
     */
    public boolean isSyntheticFollowEnabled() {
        Boolean value = syntheticFollowEnabled == null ? synteticFollowEnabled : syntheticFollowEnabled;
        return Boolean.TRUE.equals(value);
    }

    /**
     * Sets whether synthetic follow output is enabled.
     *
     * @param syntheticFollowEnabled synthetic follow state
     */
    public void setSyntheticFollowEnabled(boolean syntheticFollowEnabled) {
        this.syntheticFollowEnabled = syntheticFollowEnabled;
        this.synteticFollowEnabled = syntheticFollowEnabled;
    }

    /**
     * Returns whether synthetic join output is enabled.
     *
     * @return synthetic join state
     */
    public boolean isSyntheticJoinEnabled() {
        Boolean value = syntheticJoinEnabled == null ? synteticJoinEnabled : syntheticJoinEnabled;
        return Boolean.TRUE.equals(value);
    }

    /**
     * Sets whether synthetic join output is enabled.
     *
     * @param syntheticJoinEnabled synthetic join state
     */
    public void setSyntheticJoinEnabled(boolean syntheticJoinEnabled) {
        this.syntheticJoinEnabled = syntheticJoinEnabled;
        this.synteticJoinEnabled = syntheticJoinEnabled;
    }

    /**
     * Returns whether synthetic member-level output is enabled.
     *
     * @return synthetic member-level state
     */
    public boolean isSyntheticMemberLevelEnabled() {
        Boolean value = syntheticMemberLevelEnabled == null ? synteticMemberLevelEnabled : syntheticMemberLevelEnabled;
        return Boolean.TRUE.equals(value);
    }

    /**
     * Sets whether synthetic member-level output is enabled.
     *
     * @param syntheticMemberLevelEnabled synthetic member-level state
     */
    public void setSyntheticMemberLevelEnabled(boolean syntheticMemberLevelEnabled) {
        this.syntheticMemberLevelEnabled = syntheticMemberLevelEnabled;
        this.synteticMemberLevelEnabled = syntheticMemberLevelEnabled;
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
     * Returns the chat message template.
     *
     * @return configured chat template
     */
    public String getChatFormat() {
        return chatFormat;
    }

    /**
     * Sets the chat message template, falling back to the default when invalid.
     *
     * @param chatFormat chat template to store
     */
    public void setChatFormat(String chatFormat) {
        this.chatFormat = ChatFormatTemplate.sanitize(chatFormat);
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

    /**
     * Returns whether mirrored TikTok lines should be written through Minecraft's chat logger.
     *
     * @return chat log state
     */
    public boolean isChatLogEnabled() {
        return chatLogEnabled;
    }

    /**
     * Sets whether mirrored TikTok lines should be written through Minecraft's chat logger.
     *
     * @param chatLogEnabled chat log state
     */
    public void setChatLogEnabled(boolean chatLogEnabled) {
        this.chatLogEnabled = chatLogEnabled;
    }
}
