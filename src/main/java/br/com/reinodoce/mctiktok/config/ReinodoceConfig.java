package br.com.reinodoce.mctiktok.config;

import br.com.reinodoce.mctiktok.rules.GiftComboMode;

@SuppressWarnings("PMD.DataClass")
public class ReinodoceConfig {
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

    public static ReinodoceConfig defaults() {
        return new ReinodoceConfig();
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
        copy.setChatPrefix(chatPrefix);
        copy.setChatEmotesEnabled(chatEmotesEnabled);
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
        this.synteticGiftComboMode = GiftComboMode.fromString(synteticGiftComboMode).id();
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

    public boolean isChatEmotesEnabled() {
        return chatEmotesEnabled;
    }

    public void setChatEmotesEnabled(boolean chatEmotesEnabled) {
        this.chatEmotesEnabled = chatEmotesEnabled;
    }
}
