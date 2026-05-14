package br.com.reinodoce.mctiktok.config;

import com.google.gson.annotations.SerializedName;

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
    @SerializedName(value = "syntheticGiftMinValue", alternate = {"synteticGiftMinValue"})
    private int syntheticGiftMinValue = DEFAULT_SYNTHETIC_GIFT_MIN_VALUE;
    @SerializedName(value = "syntheticGiftComboMode", alternate = {"synteticGiftComboMode"})
    private String syntheticGiftComboMode = DEFAULT_GIFT_COMBO_MODE;
    @SerializedName(value = "syntheticFollowEnabled", alternate = {"synteticFollowEnabled"})
    private boolean syntheticFollowEnabled;
    @SerializedName(value = "syntheticJoinEnabled", alternate = {"synteticJoinEnabled"})
    private boolean syntheticJoinEnabled;
    @SerializedName(value = "syntheticMemberLevelEnabled", alternate = {"synteticMemberLevelEnabled"})
    private boolean syntheticMemberLevelEnabled;
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
        copy.setSyntheticGiftMinValue(syntheticGiftMinValue);
        copy.setSyntheticGiftComboMode(syntheticGiftComboMode);
        copy.setSyntheticFollowEnabled(syntheticFollowEnabled);
        copy.setSyntheticJoinEnabled(syntheticJoinEnabled);
        copy.setSyntheticMemberLevelEnabled(syntheticMemberLevelEnabled);
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
        return syntheticGiftMinValue;
    }

    public void setSyntheticGiftMinValue(int syntheticGiftMinValue) {
        this.syntheticGiftMinValue = Math.max(0, syntheticGiftMinValue);
    }

    public String getSyntheticGiftComboMode() {
        return syntheticGiftComboMode;
    }

    public void setSyntheticGiftComboMode(String syntheticGiftComboMode) {
        this.syntheticGiftComboMode = normalizedGiftComboMode(syntheticGiftComboMode);
    }

    public boolean isSyntheticFollowEnabled() {
        return syntheticFollowEnabled;
    }

    public void setSyntheticFollowEnabled(boolean syntheticFollowEnabled) {
        this.syntheticFollowEnabled = syntheticFollowEnabled;
    }

    public boolean isSyntheticJoinEnabled() {
        return syntheticJoinEnabled;
    }

    public void setSyntheticJoinEnabled(boolean syntheticJoinEnabled) {
        this.syntheticJoinEnabled = syntheticJoinEnabled;
    }

    public boolean isSyntheticMemberLevelEnabled() {
        return syntheticMemberLevelEnabled;
    }

    public void setSyntheticMemberLevelEnabled(boolean syntheticMemberLevelEnabled) {
        this.syntheticMemberLevelEnabled = syntheticMemberLevelEnabled;
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
