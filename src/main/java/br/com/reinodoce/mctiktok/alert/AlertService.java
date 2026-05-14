package br.com.reinodoce.mctiktok.alert;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.i18n.Translations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Converts accepted TikTok LIVE events into local-only sound and toast alerts.
 */
public class AlertService {
    private static final Duration DEFAULT_ALERT_COOLDOWN = Duration.ofSeconds(2);
    private static final Duration JOIN_ALERT_COOLDOWN = Duration.ofSeconds(10);
    private static final String GIFT_KEY = "gift:";
    private static final String FOLLOW_KEY = "follow:";
    private static final String JOIN_KEY = "join";
    private static final String MEMBER_LEVEL_KEY = "member-level:";

    private final AlertSink alertSink;
    private final Clock clock;
    private final Map<String, Instant> lastAlertByKey = new HashMap<>();

    /**
     * Creates an alert service.
     *
     * @param alertSink platform alert sink
     */
    public AlertService(AlertSink alertSink) {
        this(alertSink, Clock.systemUTC());
    }

    AlertService(AlertSink alertSink, Clock clock) {
        this.alertSink = Objects.requireNonNull(alertSink, "alertSink");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Emits a gift alert when configured thresholds allow it.
     *
     * @param config active configuration
     * @param username sender username
     * @param giftName gift display name
     * @param count emitted gift count
     * @param diamondCost single gift diamond value
     */
    public void gift(ReinodoceConfig config, String username, String giftName, int count, int diamondCost) {
        if (!isEnabled(config, AlertEventType.GIFT)
                || Math.max(0, diamondCost) < config.getAlertGiftMinValue()) {
            return;
        }
        int safeCount = Math.max(1, count);
        String safeUsername = safe(username);
        String safeGiftName = safe(giftName);
        emit(
                config,
                AlertEventType.GIFT,
                GIFT_KEY + safeUsername + ':' + safeGiftName + ':' + safeCount,
                Translations.tr("reinodoce.alert.title.gift"),
                Translations.tr("reinodoce.alert.gift", safeUsername, safeGiftName, safeCount),
                DEFAULT_ALERT_COOLDOWN);
    }

    /**
     * Emits a follow alert when enabled.
     *
     * @param config active configuration
     * @param username follower username
     */
    public void follow(ReinodoceConfig config, String username) {
        if (!isEnabled(config, AlertEventType.FOLLOW)) {
            return;
        }
        String safeUsername = safe(username);
        emit(
                config,
                AlertEventType.FOLLOW,
                FOLLOW_KEY + safeUsername,
                Translations.tr("reinodoce.alert.title.follow"),
                Translations.tr("reinodoce.alert.follow", safeUsername),
                DEFAULT_ALERT_COOLDOWN);
    }

    /**
     * Emits a join alert when enabled. Joins use a global cooldown because they can be high volume.
     *
     * @param config active configuration
     * @param username joining username
     */
    public void join(ReinodoceConfig config, String username) {
        if (!isEnabled(config, AlertEventType.JOIN)) {
            return;
        }
        emit(
                config,
                AlertEventType.JOIN,
                JOIN_KEY,
                Translations.tr("reinodoce.alert.title.join"),
                Translations.tr("reinodoce.alert.join", safe(username)),
                JOIN_ALERT_COOLDOWN);
    }

    /**
     * Emits a member-level alert when enabled.
     *
     * @param config active configuration
     * @param username member username
     * @param memberLevel new member level
     */
    public void memberLevel(ReinodoceConfig config, String username, int memberLevel) {
        if (!isEnabled(config, AlertEventType.MEMBER_LEVEL)) {
            return;
        }
        String safeUsername = safe(username);
        int safeLevel = Math.max(0, memberLevel);
        emit(
                config,
                AlertEventType.MEMBER_LEVEL,
                MEMBER_LEVEL_KEY + safeUsername + ':' + safeLevel,
                Translations.tr("reinodoce.alert.title.member_level"),
                Translations.tr("reinodoce.alert.member_level", safeUsername, safeLevel),
                DEFAULT_ALERT_COOLDOWN);
    }

    private void emit(
            ReinodoceConfig config,
            AlertEventType eventType,
            String key,
            String title,
            String message,
            Duration cooldown
    ) {
        if (!shouldEmit(key, cooldown)) {
            return;
        }
        if (config.isAlertSoundEnabled(eventType)) {
            alertSink.playAlertSound();
        }
        if (config.isAlertToastEnabled(eventType)) {
            alertSink.showAlertToast(title, message);
        }
    }

    private static boolean isEnabled(ReinodoceConfig config, AlertEventType eventType) {
        return config != null
                && (config.isAlertSoundEnabled(eventType) || config.isAlertToastEnabled(eventType));
    }

    private synchronized boolean shouldEmit(String key, Duration cooldown) {
        Instant now = clock.instant();
        Instant lastAt = lastAlertByKey.get(key);
        if (lastAt == null || Duration.between(lastAt, now).compareTo(cooldown) >= 0) {
            lastAlertByKey.put(key, now);
            return true;
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
