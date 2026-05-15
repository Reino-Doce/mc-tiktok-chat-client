package br.com.reinodoce.mctiktok.diagnostics;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the sanitized configuration portion of a diagnostics report.
 */
final class DiagnosticsConfigReport {
    private static final String KEY_CUSTOM_IMAGE = "customImage";
    private static final String KEY_MEDIA_MODE = "mediaMode";
    private static final String KEY_SOUND_ENABLED = "soundEnabled";
    private static final String KEY_SOUND_ID = "soundId";
    private static final String KEY_TOAST_ENABLED = "toastEnabled";
    private static final String KEY_TOAST_TEMPLATE = "toastTemplate";

    private DiagnosticsConfigReport() {
    }

    static Map<String, Object> from(
            ReinodoceConfig config, String effectiveLanguage, Collection<String> knownUsernames
    ) {
        Map<String, Object> output = ordered();
        output.put("connection", connectionConfig(config, effectiveLanguage));
        output.put("rules", ruleConfig(config));
        output.put("output", outputConfig(config));
        output.put("chat", chatConfig(config, knownUsernames));
        output.put("synthetic", syntheticConfig(config));
        output.put("alerts", alertConfigs(config, knownUsernames));
        output.put("sessionLogging", sessionLoggingConfig(config));
        return output;
    }

    private static Map<String, Object> connectionConfig(ReinodoceConfig config, String effectiveLanguage) {
        Map<String, Object> connection = ordered();
        connection.put("lastUsername", DiagnosticsSanitizer.username(config.getLastUsername()));
        connection.put("autoConnectOnStart", config.isAutoConnectOnStart());
        connection.put("reconnectSeconds", config.getReconnectSeconds());
        connection.put("language", config.getLanguage());
        connection.put("effectiveLanguage", effectiveLanguage);
        return connection;
    }

    private static Map<String, Object> ruleConfig(ReinodoceConfig config) {
        Map<String, Object> rules = ordered();
        rules.put("followerOnly", config.isRuleFollowerOnly());
        rules.put("minMemberLevel", config.getRuleMinMemberLevel());
        rules.put("blockedWordsCount", config.getRuleBlockedWords().size());
        rules.put("blockedUsersCount", config.getRuleBlockedUsers().size());
        rules.put("emoteOnlyFilterEnabled", config.isRuleEmoteOnlyFilterEnabled());
        rules.put("linkFilterEnabled", config.isRuleLinkFilterEnabled());
        rules.put("userCooldownSeconds", config.getRuleUserCooldownSeconds());
        rules.put("allowlistMode", config.isRuleAllowlistMode());
        rules.put("allowedUsersCount", config.getRuleAllowedUsers().size());
        rules.put("maxMessageLength", config.getRuleMaxMessageLength());
        rules.put("duplicateCooldownSeconds", config.getRuleDuplicateCooldownSeconds());
        return rules;
    }

    private static Map<String, Object> outputConfig(ReinodoceConfig config) {
        Map<String, Object> output = ordered();
        output.put("mode", config.getOutputMode());
        output.put("hudPosition", config.getHudPosition());
        output.put("hudLines", config.getHudLines());
        output.put("maskUsernamesInOutput", config.isMaskUsernamesInOutput());
        output.put("pinnedOverlayEnabled", config.isPinnedOverlayEnabled());
        output.put("pinnedOverlayPosition", config.getPinnedOverlayPosition());
        output.put("pinnedOverlayMessages", config.getPinnedOverlayMessages());
        output.put("pinnedMessagesInOutput", config.isPinnedMessagesInOutput());
        return output;
    }

    private static Map<String, Object> chatConfig(ReinodoceConfig config, Collection<String> knownUsernames) {
        Map<String, Object> chat = ordered();
        chat.put("prefix", DiagnosticsSanitizer.text(config.getChatPrefix(), knownUsernames));
        chat.put("format", DiagnosticsSanitizer.text(config.getChatFormat(), knownUsernames));
        chat.put("emotesEnabled", config.isChatEmotesEnabled());
        chat.put("minecraftChatLogEnabled", config.isChatLogEnabled());
        return chat;
    }

    private static Map<String, Object> syntheticConfig(ReinodoceConfig config) {
        Map<String, Object> synthetic = ordered();
        synthetic.put("giftMinValue", config.getSyntheticGiftMinValue());
        synthetic.put("giftComboMode", config.getSyntheticGiftComboMode());
        synthetic.put("followEnabled", config.isSyntheticFollowEnabled());
        synthetic.put("joinEnabled", config.isSyntheticJoinEnabled());
        synthetic.put("memberLevelEnabled", config.isSyntheticMemberLevelEnabled());
        return synthetic;
    }

    private static Map<String, Object> alertConfigs(
            ReinodoceConfig config, Collection<String> knownUsernames
    ) {
        Map<String, Object> alerts = ordered();
        alerts.put(AlertEventType.GIFT.id(), alertConfig(config, AlertEventType.GIFT, knownUsernames));
        alerts.put(AlertEventType.FOLLOW.id(), alertConfig(config, AlertEventType.FOLLOW, knownUsernames));
        alerts.put(AlertEventType.JOIN.id(), alertConfig(config, AlertEventType.JOIN, knownUsernames));
        alerts.put(AlertEventType.MEMBER_LEVEL.id(), alertConfig(config, AlertEventType.MEMBER_LEVEL, knownUsernames));
        return alerts;
    }

    private static Map<String, Object> alertConfig(
            ReinodoceConfig config, AlertEventType eventType, Collection<String> knownUsernames
    ) {
        Map<String, Object> alert = ordered();
        alert.put(KEY_SOUND_ENABLED, config.isAlertSoundEnabled(eventType));
        alert.put(KEY_SOUND_ID, DiagnosticsSanitizer.text(config.getAlertSoundId(eventType), knownUsernames));
        alert.put(KEY_TOAST_ENABLED, config.isAlertToastEnabled(eventType));
        alert.put(KEY_TOAST_TEMPLATE, DiagnosticsSanitizer.text(config.getAlertToastTemplate(eventType), knownUsernames));
        alert.put(KEY_MEDIA_MODE, config.getAlertMediaMode(eventType));
        alert.put(KEY_CUSTOM_IMAGE, DiagnosticsSanitizer.resource(config.getAlertCustomImage(eventType), knownUsernames));
        if (eventType == AlertEventType.GIFT) {
            alert.put("giftMinValue", config.getAlertGiftMinValue());
        }
        return alert;
    }

    private static Map<String, Object> sessionLoggingConfig(ReinodoceConfig config) {
        Map<String, Object> sessionLogging = ordered();
        sessionLogging.put("enabled", config.isSessionLoggingEnabled());
        sessionLogging.put("format", config.getSessionLoggingFormat());
        sessionLogging.put("contentIncluded", false);
        sessionLogging.put("retentionDays", config.getSessionLoggingRetentionDays());
        sessionLogging.put("retentionFiles", config.getSessionLoggingRetentionFiles());
        sessionLogging.put("anonymized", config.isSessionLoggingAnonymized());
        sessionLogging.put("metadataOnly", config.isSessionLoggingMetadataOnly());
        return sessionLogging;
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
