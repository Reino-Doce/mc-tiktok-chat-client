package br.com.reinodoce.mctiktok.config;

import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import br.com.reinodoce.mctiktok.util.UsernameValidator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Persisted client configuration for the `/reinodoce` command surface.
 */
// Config intentionally mirrors persisted JSON fields and validation rules.
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.DataClass", "PMD.GodClass", "PMD.TooManyFields"})
public class ReinodoceConfig {
    /** Default prefix used before rendered LIVE chat lines. */
    public static final String DEFAULT_CHAT_PREFIX = "[LIVE]";
    /** Default template preserving the existing LIVE chat line layout. */
    public static final String DEFAULT_CHAT_FORMAT = "{prefix}  <{username}> {message}";
    private static final int DEFAULT_RECONNECT_SECONDS = 5;
    private static final int DEFAULT_SYNTHETIC_GIFT_MIN_VALUE = 1;

    private String lastUsername = "";
    private boolean autoConnectOnStart = false;
    private int reconnectSeconds = DEFAULT_RECONNECT_SECONDS;
    private boolean ruleFollowerOnly = false;
    private int ruleMinMemberLevel = 0;
    private List<String> ruleBlockedWords = new ArrayList<>();
    private List<String> ruleBlockedUsers = new ArrayList<>();
    private int ruleMaxMessageLength = 0;
    private int ruleDuplicateCooldownSeconds = 0;
    private int syntheticGiftMinValue = DEFAULT_SYNTHETIC_GIFT_MIN_VALUE;
    private String syntheticGiftComboMode = GiftComboMode.BULK.id();
    private boolean syntheticFollowEnabled = false;
    private boolean syntheticJoinEnabled = false;
    private boolean syntheticMemberLevelEnabled = false;
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
        copy.setAutoConnectOnStart(autoConnectOnStart);
        copy.setReconnectSeconds(reconnectSeconds);
        copy.setRuleFollowerOnly(ruleFollowerOnly);
        copy.setRuleMinMemberLevel(ruleMinMemberLevel);
        copy.setRuleBlockedWords(ruleBlockedWords);
        copy.setRuleBlockedUsers(ruleBlockedUsers);
        copy.setRuleMaxMessageLength(ruleMaxMessageLength);
        copy.setRuleDuplicateCooldownSeconds(ruleDuplicateCooldownSeconds);
        copy.setSyntheticGiftMinValue(syntheticGiftMinValue);
        copy.setSyntheticGiftComboMode(syntheticGiftComboMode);
        copy.setSyntheticFollowEnabled(syntheticFollowEnabled);
        copy.setSyntheticJoinEnabled(syntheticJoinEnabled);
        copy.setSyntheticMemberLevelEnabled(syntheticMemberLevelEnabled);
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
     * Returns whether the client should connect to the saved username on startup.
     *
     * @return auto-connect setting
     */
    public boolean isAutoConnectOnStart() {
        return autoConnectOnStart;
    }

    /**
     * Sets whether the client should connect to the saved username on startup.
     *
     * @param autoConnectOnStart auto-connect setting
     */
    public void setAutoConnectOnStart(boolean autoConnectOnStart) {
        this.autoConnectOnStart = autoConnectOnStart;
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
     * Returns configured blocked word fragments.
     *
     * @return blocked words
     */
    public List<String> getRuleBlockedWords() {
        return List.copyOf(ruleBlockedWords);
    }

    /**
     * Replaces configured blocked word fragments.
     *
     * @param ruleBlockedWords blocked word fragments
     */
    public void setRuleBlockedWords(List<String> ruleBlockedWords) {
        this.ruleBlockedWords = normalizeTerms(ruleBlockedWords);
    }

    /**
     * Adds a blocked word fragment.
     *
     * @param word blocked word fragment
     * @return normalized word, or blank when invalid
     */
    public String addRuleBlockedWord(String word) {
        String normalized = normalizeTerm(word);
        if (!normalized.isBlank() && !ruleBlockedWords.contains(normalized)) {
            ruleBlockedWords = append(ruleBlockedWords, normalized);
        }
        return normalized;
    }

    /**
     * Removes a blocked word fragment.
     *
     * @param word blocked word fragment
     * @return normalized word, or blank when invalid
     */
    public String removeRuleBlockedWord(String word) {
        String normalized = normalizeTerm(word);
        ruleBlockedWords = remove(ruleBlockedWords, normalized);
        return normalized;
    }

    /**
     * Returns configured blocked usernames.
     *
     * @return blocked usernames
     */
    public List<String> getRuleBlockedUsers() {
        return List.copyOf(ruleBlockedUsers);
    }

    /**
     * Replaces configured blocked usernames.
     *
     * @param ruleBlockedUsers blocked usernames
     */
    public void setRuleBlockedUsers(List<String> ruleBlockedUsers) {
        this.ruleBlockedUsers = normalizeUsernames(ruleBlockedUsers);
    }

    /**
     * Adds a blocked username.
     *
     * @param username blocked username
     * @return normalized username, or blank when invalid
     */
    public String addRuleBlockedUser(String username) {
        String normalized = normalizeUsername(username);
        if (!normalized.isBlank() && !ruleBlockedUsers.contains(normalized)) {
            ruleBlockedUsers = append(ruleBlockedUsers, normalized);
        }
        return normalized;
    }

    /**
     * Removes a blocked username.
     *
     * @param username blocked username
     * @return normalized username, or blank when invalid
     */
    public String removeRuleBlockedUser(String username) {
        String normalized = normalizeUsername(username);
        ruleBlockedUsers = remove(ruleBlockedUsers, normalized);
        return normalized;
    }

    /**
     * Returns the maximum accepted chat message length.
     *
     * @return maximum message length, or zero when disabled
     */
    public int getRuleMaxMessageLength() {
        return ruleMaxMessageLength;
    }

    /**
     * Sets the maximum accepted chat message length.
     *
     * @param ruleMaxMessageLength maximum length, clamped to zero or greater
     */
    public void setRuleMaxMessageLength(int ruleMaxMessageLength) {
        this.ruleMaxMessageLength = Math.max(0, ruleMaxMessageLength);
    }

    /**
     * Returns the duplicate message cooldown.
     *
     * @return duplicate cooldown in seconds, or zero when disabled
     */
    public int getRuleDuplicateCooldownSeconds() {
        return ruleDuplicateCooldownSeconds;
    }

    /**
     * Sets the duplicate message cooldown.
     *
     * @param ruleDuplicateCooldownSeconds cooldown seconds, clamped to zero or greater
     */
    public void setRuleDuplicateCooldownSeconds(int ruleDuplicateCooldownSeconds) {
        this.ruleDuplicateCooldownSeconds = Math.max(0, ruleDuplicateCooldownSeconds);
    }

    /**
     * Returns the minimum gift value for synthetic gift output.
     *
     * @return minimum gift value
     */
    public int getSyntheticGiftMinValue() {
        return syntheticGiftMinValue;
    }

    /**
     * Sets the minimum gift value for synthetic gift output.
     *
     * @param syntheticGiftMinValue minimum gift value, clamped to zero or greater
     */
    public void setSyntheticGiftMinValue(int syntheticGiftMinValue) {
        this.syntheticGiftMinValue = Math.max(0, syntheticGiftMinValue);
    }

    /**
     * Returns the synthetic gift combo mode identifier.
     *
     * @return persisted combo mode identifier
     */
    public String getSyntheticGiftComboMode() {
        return syntheticGiftComboMode;
    }

    /**
     * Sets the synthetic gift combo mode.
     *
     * @param syntheticGiftComboMode combo mode identifier
     */
    public void setSyntheticGiftComboMode(String syntheticGiftComboMode) {
        this.syntheticGiftComboMode = GiftComboMode.fromString(syntheticGiftComboMode).id();
    }

    /**
     * Returns whether synthetic follow output is enabled.
     *
     * @return synthetic follow state
     */
    public boolean isSyntheticFollowEnabled() {
        return syntheticFollowEnabled;
    }

    /**
     * Sets whether synthetic follow output is enabled.
     *
     * @param syntheticFollowEnabled synthetic follow state
     */
    public void setSyntheticFollowEnabled(boolean syntheticFollowEnabled) {
        this.syntheticFollowEnabled = syntheticFollowEnabled;
    }

    /**
     * Returns whether synthetic join output is enabled.
     *
     * @return synthetic join state
     */
    public boolean isSyntheticJoinEnabled() {
        return syntheticJoinEnabled;
    }

    /**
     * Sets whether synthetic join output is enabled.
     *
     * @param syntheticJoinEnabled synthetic join state
     */
    public void setSyntheticJoinEnabled(boolean syntheticJoinEnabled) {
        this.syntheticJoinEnabled = syntheticJoinEnabled;
    }

    /**
     * Returns whether synthetic member-level output is enabled.
     *
     * @return synthetic member-level state
     */
    public boolean isSyntheticMemberLevelEnabled() {
        return syntheticMemberLevelEnabled;
    }

    /**
     * Sets whether synthetic member-level output is enabled.
     *
     * @param syntheticMemberLevelEnabled synthetic member-level state
     */
    public void setSyntheticMemberLevelEnabled(boolean syntheticMemberLevelEnabled) {
        this.syntheticMemberLevelEnabled = syntheticMemberLevelEnabled;
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

    private static List<String> normalizeTerms(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String term = normalizeTerm(value);
                if (!term.isBlank()) {
                    normalized.add(term);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private static List<String> normalizeUsernames(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String username = normalizeUsername(value);
                if (!username.isBlank()) {
                    normalized.add(username);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String normalizeTerm(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String value) {
        String normalized = UsernameValidator.normalize(value).toLowerCase(Locale.ROOT);
        return UsernameValidator.isValid(normalized) ? normalized : "";
    }

    private static List<String> append(List<String> values, String value) {
        List<String> updated = new ArrayList<>(values);
        updated.add(value);
        return updated;
    }

    private static List<String> remove(List<String> values, String value) {
        List<String> updated = new ArrayList<>(values);
        updated.remove(value);
        return updated;
    }
}
