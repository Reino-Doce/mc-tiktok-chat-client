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
    private int synteticGiftMinValue = DEFAULT_SYNTHETIC_GIFT_MIN_VALUE;
    private String synteticGiftComboMode = DEFAULT_GIFT_COMBO_MODE;
    private boolean synteticFollowEnabled;
    private boolean synteticJoinEnabled;
    private boolean synteticMemberLevelEnabled;
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
        copy.setSynteticGiftMinValue(synteticGiftMinValue);
        copy.setSynteticGiftComboMode(synteticGiftComboMode);
        copy.setSynteticFollowEnabled(synteticFollowEnabled);
        copy.setSynteticJoinEnabled(synteticJoinEnabled);
        copy.setSynteticMemberLevelEnabled(synteticMemberLevelEnabled);
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

    public int getSynteticGiftMinValue() {
        return synteticGiftMinValue;
    }

    public void setSynteticGiftMinValue(int synteticGiftMinValue) {
        this.synteticGiftMinValue = Math.max(0, synteticGiftMinValue);
    }

    public String getSynteticGiftComboMode() {
        return synteticGiftComboMode;
    }

    public void setSynteticGiftComboMode(String synteticGiftComboMode) {
        this.synteticGiftComboMode = normalizedGiftComboMode(synteticGiftComboMode);
    }

    public boolean isSynteticFollowEnabled() {
        return synteticFollowEnabled;
    }

    public void setSynteticFollowEnabled(boolean synteticFollowEnabled) {
        this.synteticFollowEnabled = synteticFollowEnabled;
    }

    public boolean isSynteticJoinEnabled() {
        return synteticJoinEnabled;
    }

    public void setSynteticJoinEnabled(boolean synteticJoinEnabled) {
        this.synteticJoinEnabled = synteticJoinEnabled;
    }

    public boolean isSynteticMemberLevelEnabled() {
        return synteticMemberLevelEnabled;
    }

    public void setSynteticMemberLevelEnabled(boolean synteticMemberLevelEnabled) {
        this.synteticMemberLevelEnabled = synteticMemberLevelEnabled;
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
