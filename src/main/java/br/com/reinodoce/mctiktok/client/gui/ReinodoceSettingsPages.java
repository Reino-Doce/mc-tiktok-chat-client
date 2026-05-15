package br.com.reinodoce.mctiktok.client.gui;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.alert.AlertToastMediaMode;
import br.com.reinodoce.mctiktok.config.HudPosition;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.logging.SessionLogFormat;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class ReinodoceSettingsPages {
    private static final int ROOT_LEFT_COLUMN = 0;
    private static final int ROOT_RIGHT_COLUMN = 1;
    private static final int ROOT_ROW_0 = 0;
    private static final int ROOT_ROW_1 = 1;
    private static final int ROOT_ROW_2 = 2;
    private static final int ROOT_ROW_3 = 3;
    private static final int TEXT_MAX_LENGTH = 80;
    private static final int FORMAT_MAX_LENGTH = 160;
    private static final int CSV_MAX_LENGTH = 256;
    private static final int IMAGE_MAX_LENGTH = 512;
    private static final int RESOURCE_ID_MAX_LENGTH = 256;
    private static final int LANGUAGE_MAX_LENGTH = 16;
    private static final List<String> OUTPUT_MODES = valuesOf(OutputMode.ids());
    private static final List<String> HUD_POSITIONS = valuesOf(HudPosition.ids());
    private static final List<String> GIFT_COMBO_MODES = List.copyOf(GiftComboMode.ids());
    private static final List<String> ALERT_MEDIA_MODES = List.of(AlertToastMediaMode.ids());
    private static final List<String> SESSION_LOG_FORMATS = SessionLogFormat.ids();

    private ReinodoceSettingsPages() {
    }

    static void init(Page page, ReinodoceSettingsScreen screen) {
        page.init(screen);
    }

    private static void initRoot(ReinodoceSettingsScreen screen) {
        screen.addDomainButton(ROOT_LEFT_COLUMN, ROOT_ROW_0, Page.CONNECTION);
        screen.addDomainButton(ROOT_RIGHT_COLUMN, ROOT_ROW_0, Page.OUTPUT);
        screen.addDomainButton(ROOT_LEFT_COLUMN, ROOT_ROW_1, Page.CHAT);
        screen.addDomainButton(ROOT_RIGHT_COLUMN, ROOT_ROW_1, Page.RULES);
        screen.addDomainButton(ROOT_LEFT_COLUMN, ROOT_ROW_2, Page.SYNTHETIC);
        screen.addDomainButton(ROOT_RIGHT_COLUMN, ROOT_ROW_2, Page.ALERTS);
        screen.addDomainButton(ROOT_LEFT_COLUMN, ROOT_ROW_3, Page.LOGGING);
        screen.addDomainButton(ROOT_RIGHT_COLUMN, ROOT_ROW_3, Page.LANGUAGE);
        screen.addRootButtons();
    }

    private static void initConnection(ReinodoceSettingsScreen screen) {
        int row = 0;
        screen.addTextField(row++, "Last user", TEXT_MAX_LENGTH, screen.configDraft()::getLastUsername,
                screen.configDraft()::setLastUsername);
        screen.addToggle(row++, "Auto-connect", screen.configDraft()::isAutoConnectOnStart,
                screen.configDraft()::setAutoConnectOnStart);
        screen.addNumberField(row, "Reconnect", screen.configDraft()::getReconnectSeconds,
                screen.configDraft()::setReconnectSeconds);
        screen.addBackButton();
    }

    private static void initOutput(ReinodoceSettingsScreen screen) {
        int row = 0;
        screen.addCycle(row++, "Output", screen.configDraft()::getOutputMode, screen.configDraft()::setOutputMode,
                OUTPUT_MODES);
        screen.addCycle(row++, "HUD position", screen.configDraft()::getHudPosition,
                screen.configDraft()::setHudPosition,
                HUD_POSITIONS);
        screen.addNumberField(row++, "HUD lines", screen.configDraft()::getHudLines,
                screen.configDraft()::setHudLines);
        screen.addToggle(row++, "Pinned overlay", screen.configDraft()::isPinnedOverlayEnabled,
                screen.configDraft()::setPinnedOverlayEnabled);
        screen.addCycle(row++, "Pinned position", screen.configDraft()::getPinnedOverlayPosition,
                screen.configDraft()::setPinnedOverlayPosition,
                HUD_POSITIONS);
        screen.addNumberField(row++, "Pinned messages", screen.configDraft()::getPinnedOverlayMessages,
                screen.configDraft()::setPinnedOverlayMessages);
        screen.addToggle(row++, "Pinned to output", screen.configDraft()::isPinnedMessagesInOutput,
                screen.configDraft()::setPinnedMessagesInOutput);
        screen.addToggle(row, "Chat log", screen.configDraft()::isChatLogEnabled,
                screen.configDraft()::setChatLogEnabled);
        screen.addBackButton();
    }

    private static void initChat(ReinodoceSettingsScreen screen) {
        int row = 0;
        screen.addTextField(row++, "Chat prefix", TEXT_MAX_LENGTH, screen.configDraft()::getChatPrefix,
                screen.configDraft()::setChatPrefix);
        screen.addTextField(row++, "Chat format", FORMAT_MAX_LENGTH, screen.configDraft()::getChatFormat,
                screen.configDraft()::setChatFormat);
        screen.addToggle(row++, "Chat emotes", screen.configDraft()::isChatEmotesEnabled,
                screen.configDraft()::setChatEmotesEnabled);
        screen.addToggle(row, "Mask usernames", screen.configDraft()::isMaskUsernamesInOutput,
                screen.configDraft()::setMaskUsernamesInOutput);
        screen.addBackButton();
    }

    private static void initRules(ReinodoceSettingsScreen screen) {
        int row = 0;
        screen.addToggle(row++, "Follower only", screen.configDraft()::isRuleFollowerOnly,
                screen.configDraft()::setRuleFollowerOnly);
        screen.addNumberField(row++, "Min level", screen.configDraft()::getRuleMinMemberLevel,
                screen.configDraft()::setRuleMinMemberLevel);
        screen.addNumberField(row++, "Max length", screen.configDraft()::getRuleMaxMessageLength,
                screen.configDraft()::setRuleMaxMessageLength);
        screen.addNumberField(row++, "Duplicate sec", screen.configDraft()::getRuleDuplicateCooldownSeconds,
                screen.configDraft()::setRuleDuplicateCooldownSeconds);
        screen.addTextField(row++, "Blocked words", CSV_MAX_LENGTH,
                () -> ReinodoceSettingsScreen.joinCsv(screen.configDraft().getRuleBlockedWords()),
                value -> screen.configDraft().setRuleBlockedWords(ReinodoceSettingsScreen.parseCsvList(value)));
        screen.addTextField(row, "Blocked users", CSV_MAX_LENGTH,
                () -> ReinodoceSettingsScreen.joinCsv(screen.configDraft().getRuleBlockedUsers()),
                value -> screen.configDraft().setRuleBlockedUsers(ReinodoceSettingsScreen.parseCsvList(value)));
        screen.addBackButton();
    }

    private static void initSynthetic(ReinodoceSettingsScreen screen) {
        int row = 0;
        screen.addNumberField(row++, "Gift minimum", screen.configDraft()::getSyntheticGiftMinValue,
                screen.configDraft()::setSyntheticGiftMinValue);
        screen.addCycle(row++, "Gift combo", screen.configDraft()::getSyntheticGiftComboMode,
                screen.configDraft()::setSyntheticGiftComboMode, GIFT_COMBO_MODES);
        screen.addToggle(row++, "Follow event", screen.configDraft()::isSyntheticFollowEnabled,
                screen.configDraft()::setSyntheticFollowEnabled);
        screen.addToggle(row++, "Join event", screen.configDraft()::isSyntheticJoinEnabled,
                screen.configDraft()::setSyntheticJoinEnabled);
        screen.addToggle(row, "Member event", screen.configDraft()::isSyntheticMemberLevelEnabled,
                screen.configDraft()::setSyntheticMemberLevelEnabled);
        screen.addBackButton();
    }

    private static void initAlerts(ReinodoceSettingsScreen screen) {
        screen.addDomainButton(ROOT_LEFT_COLUMN, ROOT_ROW_0, Page.ALERT_GIFT);
        screen.addDomainButton(ROOT_RIGHT_COLUMN, ROOT_ROW_0, Page.ALERT_FOLLOW);
        screen.addDomainButton(ROOT_LEFT_COLUMN, ROOT_ROW_1, Page.ALERT_JOIN);
        screen.addDomainButton(ROOT_RIGHT_COLUMN, ROOT_ROW_1, Page.ALERT_MEMBER_LEVEL);
        screen.addBackButton();
    }

    private static void initAlertEvent(ReinodoceSettingsScreen screen, AlertEventType eventType) {
        int row = 0;
        screen.addToggle(row++, "Sound", () -> screen.configDraft().isAlertSoundEnabled(eventType),
                enabled -> screen.configDraft().setAlertSoundEnabled(eventType, enabled));
        screen.addTextField(row++, "Sound id", RESOURCE_ID_MAX_LENGTH,
                () -> screen.configDraft().getAlertSoundId(eventType),
                value -> screen.configDraft().setAlertSoundId(eventType, value));
        screen.addToggle(row++, "Toast", () -> screen.configDraft().isAlertToastEnabled(eventType),
                enabled -> screen.configDraft().setAlertToastEnabled(eventType, enabled));
        screen.addTextField(row++, "Template", FORMAT_MAX_LENGTH,
                () -> screen.configDraft().getAlertToastTemplate(eventType),
                value -> screen.configDraft().setAlertToastTemplate(eventType, value));
        screen.addCycle(row++, "Media mode", () -> screen.configDraft().getAlertMediaMode(eventType),
                value -> screen.configDraft().setAlertMediaMode(eventType, value), ALERT_MEDIA_MODES);
        screen.addTextField(row++, "Custom image", IMAGE_MAX_LENGTH,
                () -> screen.configDraft().getAlertCustomImage(eventType),
                value -> screen.configDraft().setAlertCustomImage(eventType, value));
        if (eventType == AlertEventType.GIFT) {
            screen.addNumberField(row, "Min value", screen.configDraft()::getAlertGiftMinValue,
                    screen.configDraft()::setAlertGiftMinValue);
        }
        screen.addBackButton();
    }

    private static void initLogging(ReinodoceSettingsScreen screen) {
        int row = 0;
        screen.addToggle(row++, "Enabled", screen.configDraft()::isSessionLoggingEnabled,
                screen.configDraft()::setSessionLoggingEnabled);
        screen.addCycle(row++, "Format", screen.configDraft()::getSessionLoggingFormat,
                screen.configDraft()::setSessionLoggingFormat, SESSION_LOG_FORMATS);
        screen.addToggle(row++, "Anonymized", screen.configDraft()::isSessionLoggingAnonymized,
                screen.configDraft()::setSessionLoggingAnonymized);
        screen.addToggle(row++, "Metadata only", screen.configDraft()::isSessionLoggingMetadataOnly,
                screen.configDraft()::setSessionLoggingMetadataOnly);
        screen.addNumberField(row++, "Retention days", screen.configDraft()::getSessionLoggingRetentionDays,
                screen.configDraft()::setSessionLoggingRetentionDays);
        screen.addNumberField(row, "Retention files", screen.configDraft()::getSessionLoggingRetentionFiles,
                screen.configDraft()::setSessionLoggingRetentionFiles);
        screen.addBackButton();
    }

    private static void initLanguage(ReinodoceSettingsScreen screen) {
        screen.addTextField(0, "Language", LANGUAGE_MAX_LENGTH, screen.configDraft()::getLanguage,
                screen.configDraft()::setLanguage);
        screen.addBackButton();
    }

    private static List<String> valuesOf(Iterable<String> values) {
        List<String> ids = new ArrayList<>();
        values.forEach(ids::add);
        return List.copyOf(ids);
    }

    enum Page {
        ROOT("Reino Doce TikTok Settings", "Choose a settings area. Changes save only when you press Done.",
                "Settings index", ReinodoceSettingsPages::initRoot),
        CONNECTION("Connection", "Connection defaults and reconnect behavior.", "Connection",
                ReinodoceSettingsPages::initConnection),
        OUTPUT("Output / HUD", "Visible output destination, overlays, HUD placement, and chat logging.", "Output / HUD",
                ReinodoceSettingsPages::initOutput),
        CHAT("Chat formatting", "LIVE prefix, rendered chat template, and inline emotes.", "Chat formatting",
                ReinodoceSettingsPages::initChat),
        RULES("Rules / moderation", "Filtering rules plus comma-separated blocked word and user lists.",
                "Rules / moderation", ReinodoceSettingsPages::initRules),
        SYNTHETIC("Synthetic events", "Non-chat LIVE events surfaced into local client output.", "Synthetic events",
                ReinodoceSettingsPages::initSynthetic),
        ALERTS("Alerts / notifications", "Choose which surfaced event type to configure for local alerts.",
                "Alerts / notifications", ReinodoceSettingsPages::initAlerts),
        ALERT_GIFT("Gift alerts", "Client-only gift alert sound, toast, media, and minimum diamond value.",
                "Gift alerts", screen -> initAlertEvent(screen, AlertEventType.GIFT)),
        ALERT_FOLLOW("Follow alerts", "Client-only follow alert sound, toast, media, and template.",
                "Follow alerts", screen -> initAlertEvent(screen, AlertEventType.FOLLOW)),
        ALERT_JOIN("Join alerts", "Client-only join alert sound, toast, media, and template.", "Join alerts",
                screen -> initAlertEvent(screen, AlertEventType.JOIN)),
        ALERT_MEMBER_LEVEL("Member-level alerts",
                "Client-only member-level alert sound, toast, media, and template.", "Member-level alerts",
                screen -> initAlertEvent(screen, AlertEventType.MEMBER_LEVEL)),
        LOGGING("Session logging", "Opt-in local session files written under logs/reinodoce/.", "Session logging",
                ReinodoceSettingsPages::initLogging),
        LANGUAGE("Language / localization", "Use auto or a locale such as pt_br to override Minecraft language.",
                "Language / localization", ReinodoceSettingsPages::initLanguage);

        private final String titleText;
        private final String subtitleText;
        private final String buttonText;
        private final Consumer<ReinodoceSettingsScreen> initializer;

        Page(String title, String subtitle, String buttonText, Consumer<ReinodoceSettingsScreen> initializer) {
            this.titleText = title;
            this.subtitleText = subtitle;
            this.buttonText = buttonText;
            this.initializer = initializer;
        }

        String title() {
            return titleText;
        }

        String subtitle() {
            return subtitleText;
        }

        String buttonLabel() {
            return buttonText;
        }

        void init(ReinodoceSettingsScreen screen) {
            initializer.accept(screen);
        }
    }
}
