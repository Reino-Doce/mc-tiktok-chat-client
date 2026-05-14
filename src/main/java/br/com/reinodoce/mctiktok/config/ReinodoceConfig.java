package br.com.reinodoce.mctiktok.config;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.alert.AlertSoundId;
import br.com.reinodoce.mctiktok.alert.AlertToastMediaMode;
import br.com.reinodoce.mctiktok.alert.AlertToastTemplate;
import br.com.reinodoce.mctiktok.logging.SessionLogFormat;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import br.com.reinodoce.mctiktok.util.UsernameValidator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Persisted client configuration for the `/reinodoce` command surface.
 */
// Config intentionally mirrors persisted JSON fields and validation rules.
@SuppressWarnings({
    "PMD.CyclomaticComplexity",
    "PMD.DataClass",
    "PMD.ExcessivePublicCount",
    "PMD.GodClass",
    "PMD.TooManyFields"
})
public class ReinodoceConfig {
    /** Default prefix used before rendered LIVE chat lines. */
    public static final String DEFAULT_CHAT_PREFIX = "[LIVE]";
    /** Default template preserving the existing LIVE chat line layout. */
    public static final String DEFAULT_CHAT_FORMAT = "{prefix}  <{username}> {message}";
    /** Default HUD line count. */
    public static final int DEFAULT_HUD_LINES = 6;
    /** Maximum retained HUD line count. */
    public static final int MAX_HUD_LINES = 12;
    /** Default language setting that follows the Minecraft client language. */
    public static final String DEFAULT_LANGUAGE = LanguageSetting.AUTO;
    /** Default alert toast template, meaning built-in localized toast text. */
    public static final String DEFAULT_ALERT_TOAST_TEMPLATE = AlertToastTemplate.DEFAULT;
    private static final int DEFAULT_RECONNECT_SECONDS = 5;
    private static final int DEFAULT_SYNTHETIC_GIFT_MIN_VALUE = 1;
    private static final int DEFAULT_ALERT_GIFT_MIN_VALUE = 1;
    private static final int MAX_ALERT_CUSTOM_IMAGE_LENGTH = 512;
    private static final String ALERT_EVENT_TYPE_ARGUMENT = "eventType";
    private static final String DEFAULT_CUSTOM_IMAGE = "";

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
    private boolean alertGiftSoundEnabled = false;
    private String alertGiftSoundId = AlertSoundId.DEFAULT;
    private boolean alertGiftToastEnabled = false;
    private String alertGiftToastTemplate = DEFAULT_ALERT_TOAST_TEMPLATE;
    private String alertGiftMediaMode = AlertToastMediaMode.DEFAULT.id();
    private String alertGiftCustomImage = DEFAULT_CUSTOM_IMAGE;
    private int alertGiftMinValue = DEFAULT_ALERT_GIFT_MIN_VALUE;
    private boolean alertFollowSoundEnabled = false;
    private String alertFollowSoundId = AlertSoundId.DEFAULT;
    private boolean alertFollowToastEnabled = false;
    private String alertFollowToastTemplate = DEFAULT_ALERT_TOAST_TEMPLATE;
    private String alertFollowMediaMode = AlertToastMediaMode.DEFAULT.id();
    private String alertFollowCustomImage = DEFAULT_CUSTOM_IMAGE;
    private boolean alertJoinSoundEnabled = false;
    private String alertJoinSoundId = AlertSoundId.DEFAULT;
    private boolean alertJoinToastEnabled = false;
    private String alertJoinToastTemplate = DEFAULT_ALERT_TOAST_TEMPLATE;
    private String alertJoinMediaMode = AlertToastMediaMode.DEFAULT.id();
    private String alertJoinCustomImage = DEFAULT_CUSTOM_IMAGE;
    private boolean alertMemberLevelSoundEnabled = false;
    private String alertMemberLevelSoundId = AlertSoundId.DEFAULT;
    private boolean alertMemberLevelToastEnabled = false;
    private String alertMemberLevelToastTemplate = DEFAULT_ALERT_TOAST_TEMPLATE;
    private String alertMemberLevelMediaMode = AlertToastMediaMode.DEFAULT.id();
    private String alertMemberLevelCustomImage = DEFAULT_CUSTOM_IMAGE;
    private String outputMode = OutputMode.CHAT.id();
    private String hudPosition = HudPosition.TOP_LEFT.id();
    private int hudLines = DEFAULT_HUD_LINES;
    private String chatPrefix = DEFAULT_CHAT_PREFIX;
    private String chatFormat = DEFAULT_CHAT_FORMAT;
    private boolean chatEmotesEnabled = true;
    private boolean chatLogEnabled = false;
    private String language = DEFAULT_LANGUAGE;
    private boolean sessionLoggingEnabled = false;
    private String sessionLoggingFormat = SessionLogFormat.JSONL.id();

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
        copy.setAlertSoundEnabled(AlertEventType.GIFT, alertGiftSoundEnabled);
        copy.setAlertSoundId(AlertEventType.GIFT, alertGiftSoundId);
        copy.setAlertToastEnabled(AlertEventType.GIFT, alertGiftToastEnabled);
        copy.setAlertToastTemplate(AlertEventType.GIFT, alertGiftToastTemplate);
        copy.setAlertMediaMode(AlertEventType.GIFT, alertGiftMediaMode);
        copy.setAlertCustomImage(AlertEventType.GIFT, alertGiftCustomImage);
        copy.setAlertGiftMinValue(alertGiftMinValue);
        copy.setAlertSoundEnabled(AlertEventType.FOLLOW, alertFollowSoundEnabled);
        copy.setAlertSoundId(AlertEventType.FOLLOW, alertFollowSoundId);
        copy.setAlertToastEnabled(AlertEventType.FOLLOW, alertFollowToastEnabled);
        copy.setAlertToastTemplate(AlertEventType.FOLLOW, alertFollowToastTemplate);
        copy.setAlertMediaMode(AlertEventType.FOLLOW, alertFollowMediaMode);
        copy.setAlertCustomImage(AlertEventType.FOLLOW, alertFollowCustomImage);
        copy.setAlertSoundEnabled(AlertEventType.JOIN, alertJoinSoundEnabled);
        copy.setAlertSoundId(AlertEventType.JOIN, alertJoinSoundId);
        copy.setAlertToastEnabled(AlertEventType.JOIN, alertJoinToastEnabled);
        copy.setAlertToastTemplate(AlertEventType.JOIN, alertJoinToastTemplate);
        copy.setAlertMediaMode(AlertEventType.JOIN, alertJoinMediaMode);
        copy.setAlertCustomImage(AlertEventType.JOIN, alertJoinCustomImage);
        copy.setAlertSoundEnabled(AlertEventType.MEMBER_LEVEL, alertMemberLevelSoundEnabled);
        copy.setAlertSoundId(AlertEventType.MEMBER_LEVEL, alertMemberLevelSoundId);
        copy.setAlertToastEnabled(AlertEventType.MEMBER_LEVEL, alertMemberLevelToastEnabled);
        copy.setAlertToastTemplate(AlertEventType.MEMBER_LEVEL, alertMemberLevelToastTemplate);
        copy.setAlertMediaMode(AlertEventType.MEMBER_LEVEL, alertMemberLevelMediaMode);
        copy.setAlertCustomImage(AlertEventType.MEMBER_LEVEL, alertMemberLevelCustomImage);
        copy.setOutputMode(outputMode);
        copy.setHudPosition(hudPosition);
        copy.setHudLines(hudLines);
        copy.setChatPrefix(chatPrefix);
        copy.setChatFormat(chatFormat);
        copy.setChatEmotesEnabled(chatEmotesEnabled);
        copy.setChatLogEnabled(chatLogEnabled);
        copy.setLanguage(language);
        copy.setSessionLoggingEnabled(sessionLoggingEnabled);
        copy.setSessionLoggingFormat(sessionLoggingFormat);
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

    public boolean isAlertSoundEnabled(AlertEventType eventType) {
        return switch (requireAlertEventType(eventType)) {
            case GIFT -> alertGiftSoundEnabled;
            case FOLLOW -> alertFollowSoundEnabled;
            case JOIN -> alertJoinSoundEnabled;
            case MEMBER_LEVEL -> alertMemberLevelSoundEnabled;
            default -> false;
        };
    }

    public void setAlertSoundEnabled(AlertEventType eventType, boolean enabled) {
        switch (requireAlertEventType(eventType)) {
            case GIFT -> alertGiftSoundEnabled = enabled;
            case FOLLOW -> alertFollowSoundEnabled = enabled;
            case JOIN -> alertJoinSoundEnabled = enabled;
            case MEMBER_LEVEL -> alertMemberLevelSoundEnabled = enabled;
            default -> throw new IllegalArgumentException(ALERT_EVENT_TYPE_ARGUMENT);
        }
    }

    public String getAlertSoundId(AlertEventType eventType) {
        return switch (requireAlertEventType(eventType)) {
            case GIFT -> AlertSoundId.sanitize(alertGiftSoundId);
            case FOLLOW -> AlertSoundId.sanitize(alertFollowSoundId);
            case JOIN -> AlertSoundId.sanitize(alertJoinSoundId);
            case MEMBER_LEVEL -> AlertSoundId.sanitize(alertMemberLevelSoundId);
            default -> AlertSoundId.DEFAULT;
        };
    }

    public void setAlertSoundId(AlertEventType eventType, String soundId) {
        String sanitized = AlertSoundId.sanitize(soundId);
        switch (requireAlertEventType(eventType)) {
            case GIFT -> alertGiftSoundId = sanitized;
            case FOLLOW -> alertFollowSoundId = sanitized;
            case JOIN -> alertJoinSoundId = sanitized;
            case MEMBER_LEVEL -> alertMemberLevelSoundId = sanitized;
            default -> throw new IllegalArgumentException(ALERT_EVENT_TYPE_ARGUMENT);
        }
    }

    public boolean isAlertToastEnabled(AlertEventType eventType) {
        return switch (requireAlertEventType(eventType)) {
            case GIFT -> alertGiftToastEnabled;
            case FOLLOW -> alertFollowToastEnabled;
            case JOIN -> alertJoinToastEnabled;
            case MEMBER_LEVEL -> alertMemberLevelToastEnabled;
            default -> false;
        };
    }

    public void setAlertToastEnabled(AlertEventType eventType, boolean enabled) {
        switch (requireAlertEventType(eventType)) {
            case GIFT -> alertGiftToastEnabled = enabled;
            case FOLLOW -> alertFollowToastEnabled = enabled;
            case JOIN -> alertJoinToastEnabled = enabled;
            case MEMBER_LEVEL -> alertMemberLevelToastEnabled = enabled;
            default -> throw new IllegalArgumentException(ALERT_EVENT_TYPE_ARGUMENT);
        }
    }

    public String getAlertToastTemplate(AlertEventType eventType) {
        return switch (requireAlertEventType(eventType)) {
            case GIFT -> AlertToastTemplate.sanitize(alertGiftToastTemplate);
            case FOLLOW -> AlertToastTemplate.sanitize(alertFollowToastTemplate);
            case JOIN -> AlertToastTemplate.sanitize(alertJoinToastTemplate);
            case MEMBER_LEVEL -> AlertToastTemplate.sanitize(alertMemberLevelToastTemplate);
            default -> AlertToastTemplate.DEFAULT;
        };
    }

    public void setAlertToastTemplate(AlertEventType eventType, String template) {
        String sanitized = AlertToastTemplate.sanitize(template);
        switch (requireAlertEventType(eventType)) {
            case GIFT -> alertGiftToastTemplate = sanitized;
            case FOLLOW -> alertFollowToastTemplate = sanitized;
            case JOIN -> alertJoinToastTemplate = sanitized;
            case MEMBER_LEVEL -> alertMemberLevelToastTemplate = sanitized;
            default -> throw new IllegalArgumentException(ALERT_EVENT_TYPE_ARGUMENT);
        }
    }

    public static boolean isValidAlertToastTemplate(String template) {
        return AlertToastTemplate.isValid(template);
    }

    public static Optional<String> parseAlertMediaModeId(String mediaMode) {
        return AlertToastMediaMode.parse(mediaMode).map(AlertToastMediaMode::id);
    }

    public String getAlertMediaMode(AlertEventType eventType) {
        return switch (requireAlertEventType(eventType)) {
            case GIFT -> AlertToastMediaMode.fromString(alertGiftMediaMode).id();
            case FOLLOW -> AlertToastMediaMode.fromString(alertFollowMediaMode).id();
            case JOIN -> AlertToastMediaMode.fromString(alertJoinMediaMode).id();
            case MEMBER_LEVEL -> AlertToastMediaMode.fromString(alertMemberLevelMediaMode).id();
            default -> AlertToastMediaMode.DEFAULT.id();
        };
    }

    public void setAlertMediaMode(AlertEventType eventType, String mediaMode) {
        String sanitized = AlertToastMediaMode.fromString(mediaMode).id();
        switch (requireAlertEventType(eventType)) {
            case GIFT -> alertGiftMediaMode = sanitized;
            case FOLLOW -> alertFollowMediaMode = sanitized;
            case JOIN -> alertJoinMediaMode = sanitized;
            case MEMBER_LEVEL -> alertMemberLevelMediaMode = sanitized;
            default -> throw new IllegalArgumentException(ALERT_EVENT_TYPE_ARGUMENT);
        }
    }

    public String getAlertCustomImage(AlertEventType eventType) {
        return switch (requireAlertEventType(eventType)) {
            case GIFT -> sanitizeAlertCustomImage(alertGiftCustomImage);
            case FOLLOW -> sanitizeAlertCustomImage(alertFollowCustomImage);
            case JOIN -> sanitizeAlertCustomImage(alertJoinCustomImage);
            case MEMBER_LEVEL -> sanitizeAlertCustomImage(alertMemberLevelCustomImage);
            default -> DEFAULT_CUSTOM_IMAGE;
        };
    }

    public void setAlertCustomImage(AlertEventType eventType, String customImage) {
        String sanitized = sanitizeAlertCustomImage(customImage);
        switch (requireAlertEventType(eventType)) {
            case GIFT -> alertGiftCustomImage = sanitized;
            case FOLLOW -> alertFollowCustomImage = sanitized;
            case JOIN -> alertJoinCustomImage = sanitized;
            case MEMBER_LEVEL -> alertMemberLevelCustomImage = sanitized;
            default -> throw new IllegalArgumentException(ALERT_EVENT_TYPE_ARGUMENT);
        }
    }

    /**
     * Returns the minimum single-gift diamond cost for gift alerts.
     *
     * @return minimum gift alert value
     */
    public int getAlertGiftMinValue() {
        return alertGiftMinValue;
    }

    /**
     * Sets the minimum single-gift diamond cost for gift alerts.
     *
     * @param alertGiftMinValue minimum value, clamped to zero or greater
     */
    public void setAlertGiftMinValue(int alertGiftMinValue) {
        this.alertGiftMinValue = Math.max(0, alertGiftMinValue);
    }

    private static AlertEventType requireAlertEventType(AlertEventType eventType) {
        return Objects.requireNonNull(eventType, ALERT_EVENT_TYPE_ARGUMENT);
    }

    private static String sanitizeAlertCustomImage(String value) {
        if (value == null || value.isBlank() || "default".equalsIgnoreCase(value.trim())) {
            return DEFAULT_CUSTOM_IMAGE;
        }
        String trimmed = value.trim();
        StringBuilder builder = new StringBuilder(Math.min(trimmed.length(), MAX_ALERT_CUSTOM_IMAGE_LENGTH));
        for (int index = 0; index < trimmed.length() && builder.length() < MAX_ALERT_CUSTOM_IMAGE_LENGTH; index++) {
            char current = trimmed.charAt(index);
            if (!Character.isISOControl(current)) {
                builder.append(current);
            }
        }
        return builder.toString().trim();
    }

    /**
     * Returns the local output mode identifier.
     *
     * @return output mode id
     */
    public String getOutputMode() {
        return outputMode;
    }

    /**
     * Sets the local output mode.
     *
     * @param outputMode output mode identifier
     */
    public void setOutputMode(String outputMode) {
        this.outputMode = OutputMode.fromString(outputMode).id();
    }

    /**
     * Returns the local HUD anchor identifier.
     *
     * @return HUD position id
     */
    public String getHudPosition() {
        return hudPosition;
    }

    /**
     * Sets the local HUD anchor.
     *
     * @param hudPosition HUD position identifier
     */
    public void setHudPosition(String hudPosition) {
        this.hudPosition = HudPosition.fromString(hudPosition).id();
    }

    /**
     * Returns retained local HUD line count.
     *
     * @return HUD line count
     */
    public int getHudLines() {
        return hudLines;
    }

    /**
     * Sets retained local HUD line count.
     *
     * @param hudLines HUD line count, clamped to supported bounds
     */
    public void setHudLines(int hudLines) {
        this.hudLines = Math.max(1, Math.min(MAX_HUD_LINES, hudLines));
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

    /**
     * Returns the configured language setting.
     *
     * @return {@code auto} or a normalized locale such as {@code pt_br}
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language setting, falling back to auto when invalid.
     *
     * @param language language setting
     */
    public void setLanguage(String language) {
        this.language = LanguageSetting.sanitize(language);
    }

    /**
     * Returns whether local session event logging is enabled.
     *
     * @return local session logging state
     */
    public boolean isSessionLoggingEnabled() {
        return sessionLoggingEnabled;
    }

    /**
     * Sets whether local session event logging is enabled.
     *
     * @param sessionLoggingEnabled local session logging state
     */
    public void setSessionLoggingEnabled(boolean sessionLoggingEnabled) {
        this.sessionLoggingEnabled = sessionLoggingEnabled;
    }

    /**
     * Returns the local session event log format.
     *
     * @return session log format id
     */
    public String getSessionLoggingFormat() {
        return sessionLoggingFormat;
    }

    /**
     * Sets the local session event log format.
     *
     * @param sessionLoggingFormat session log format id
     */
    public void setSessionLoggingFormat(String sessionLoggingFormat) {
        this.sessionLoggingFormat = SessionLogFormat.fromString(sessionLoggingFormat).id();
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
