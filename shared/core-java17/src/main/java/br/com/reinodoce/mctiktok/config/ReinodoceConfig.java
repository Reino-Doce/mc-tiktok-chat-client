package br.com.reinodoce.mctiktok.config;

import java.util.Locale;

public class ReinodoceConfig {
    private static final String DEFAULT_CHAT_PREFIX = "[LIVE]";
    private static final String DEFAULT_GIFT_COMBO_MODE = "bulk";
    private static final int DEFAULT_RECONNECT_SECONDS = 5;
    private static final int DEFAULT_SYNTHETIC_GIFT_MIN_VALUE = 1;

    private String lastUsername = "";
    private int reconnectSeconds = DEFAULT_RECONNECT_SECONDS;
    private boolean ruleFollowerOnly;
    private int ruleMinMemberLevel;
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
    private boolean chatEmotesEnabled = true;
    private boolean chatLogEnabled = false;
    private String chatPrefix = DEFAULT_CHAT_PREFIX;

    public static ReinodoceConfig defaults() {
        return ReinodoceConfigDefaults.create();
    }

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
        copy.setChatEmotesEnabled(chatEmotesEnabled);
        copy.setChatLogEnabled(chatLogEnabled);
        copy.setChatPrefix(chatPrefix);
        return copy;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername == null ? "" : lastUsername.trim();
    }

    public int getReconnectSeconds() {
        return reconnectSeconds;
    }

    public void setReconnectSeconds(int reconnectSeconds) {
        this.reconnectSeconds = Math.max(0, reconnectSeconds);
    }

    public boolean isRuleFollowerOnly() {
        return ruleFollowerOnly;
    }

    public void setRuleFollowerOnly(boolean ruleFollowerOnly) {
        this.ruleFollowerOnly = ruleFollowerOnly;
    }

    public int getRuleMinMemberLevel() {
        return ruleMinMemberLevel;
    }

    public void setRuleMinMemberLevel(int ruleMinMemberLevel) {
        this.ruleMinMemberLevel = Math.max(0, ruleMinMemberLevel);
    }

    public int getSyntheticGiftMinValue() {
        Integer value = syntheticGiftMinValue == null ? synteticGiftMinValue : syntheticGiftMinValue;
        return value == null ? DEFAULT_SYNTHETIC_GIFT_MIN_VALUE : Math.max(0, value);
    }

    public void setSyntheticGiftMinValue(int syntheticGiftMinValue) {
        int normalized = Math.max(0, syntheticGiftMinValue);
        this.syntheticGiftMinValue = normalized;
        this.synteticGiftMinValue = normalized;
    }

    public String getSyntheticGiftComboMode() {
        String value = syntheticGiftComboMode == null ? synteticGiftComboMode : syntheticGiftComboMode;
        return normalizedGiftComboMode(value);
    }

    public void setSyntheticGiftComboMode(String syntheticGiftComboMode) {
        String normalized = normalizedGiftComboMode(syntheticGiftComboMode);
        this.syntheticGiftComboMode = normalized;
        this.synteticGiftComboMode = normalized;
    }

    public boolean isSyntheticFollowEnabled() {
        Boolean value = syntheticFollowEnabled == null ? synteticFollowEnabled : syntheticFollowEnabled;
        return Boolean.TRUE.equals(value);
    }

    public void setSyntheticFollowEnabled(boolean syntheticFollowEnabled) {
        this.syntheticFollowEnabled = syntheticFollowEnabled;
        this.synteticFollowEnabled = syntheticFollowEnabled;
    }

    public boolean isSyntheticJoinEnabled() {
        Boolean value = syntheticJoinEnabled == null ? synteticJoinEnabled : syntheticJoinEnabled;
        return Boolean.TRUE.equals(value);
    }

    public void setSyntheticJoinEnabled(boolean syntheticJoinEnabled) {
        this.syntheticJoinEnabled = syntheticJoinEnabled;
        this.synteticJoinEnabled = syntheticJoinEnabled;
    }

    public boolean isSyntheticMemberLevelEnabled() {
        Boolean value = syntheticMemberLevelEnabled == null ? synteticMemberLevelEnabled : syntheticMemberLevelEnabled;
        return Boolean.TRUE.equals(value);
    }

    public void setSyntheticMemberLevelEnabled(boolean syntheticMemberLevelEnabled) {
        this.syntheticMemberLevelEnabled = syntheticMemberLevelEnabled;
        this.synteticMemberLevelEnabled = syntheticMemberLevelEnabled;
    }

    public boolean isChatEmotesEnabled() {
        return chatEmotesEnabled;
    }

    public void setChatEmotesEnabled(boolean chatEmotesEnabled) {
        this.chatEmotesEnabled = chatEmotesEnabled;
    }

    public boolean isChatLogEnabled() {
        return chatLogEnabled;
    }

    public void setChatLogEnabled(boolean chatLogEnabled) {
        this.chatLogEnabled = chatLogEnabled;
    }

    public String getChatPrefix() {
        return chatPrefix;
    }

    public void setChatPrefix(String chatPrefix) {
        if (chatPrefix == null || chatPrefix.isBlank()) {
            this.chatPrefix = DEFAULT_CHAT_PREFIX;
            return;
        }
        this.chatPrefix = chatPrefix.trim();
    }

    private String normalizedGiftComboMode(String value) {
        if (value == null) {
            return DEFAULT_GIFT_COMBO_MODE;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if ("ignore".equals(normalized) || "single".equals(normalized) || DEFAULT_GIFT_COMBO_MODE.equals(normalized)) {
            return normalized;
        }
        return DEFAULT_GIFT_COMBO_MODE;
    }
}
