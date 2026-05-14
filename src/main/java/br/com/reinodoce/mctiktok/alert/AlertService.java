package br.com.reinodoce.mctiktok.alert;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;

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
        gift(config, username, giftName, count, diamondCost, AlertMediaReferences.EMPTY);
    }

    /**
     * Emits a gift alert when configured thresholds allow it.
     *
     * @param config active configuration
     * @param username sender username
     * @param giftName gift display name
     * @param count emitted gift count
     * @param diamondCost single gift diamond value
     * @param mediaReferences event media references
     */
    public void gift(
            ReinodoceConfig config,
            String username,
            String giftName,
            int count,
            int diamondCost,
            AlertMediaReferences mediaReferences
    ) {
        if (!isEnabled(config, AlertEventType.GIFT)
                || Math.max(0, diamondCost) < config.getAlertGiftMinValue()) {
            return;
        }
        int safeCount = Math.max(1, count);
        String safeUsername = safe(username);
        String safeGiftName = safe(giftName);
        long safeDiamonds = (long) safeCount * Math.max(0, diamondCost);
        emit(
                config,
                AlertEventType.GIFT,
                GIFT_KEY + safeUsername + ':' + safeGiftName + ':' + safeCount,
                new AlertToastText(
                        Translations.tr("reinodoce.alert.title.gift"),
                        Translations.tr("reinodoce.alert.gift", safeUsername, safeGiftName, safeCount)),
                new AlertToastEventData(
                        safeUsername,
                        safeGiftName,
                        safeCount,
                        safeDiamonds,
                        0,
                        media(mediaReferences)),
                DEFAULT_ALERT_COOLDOWN);
    }

    /**
     * Emits a follow alert when enabled.
     *
     * @param config active configuration
     * @param username follower username
     */
    public void follow(ReinodoceConfig config, String username) {
        follow(config, username, "");
    }

    /**
     * Emits a follow alert when enabled.
     *
     * @param config active configuration
     * @param username follower username
     * @param profileImageUrl follower profile image URL
     */
    public void follow(ReinodoceConfig config, String username, String profileImageUrl) {
        if (!isEnabled(config, AlertEventType.FOLLOW)) {
            return;
        }
        String safeUsername = safe(username);
        emit(
                config,
                AlertEventType.FOLLOW,
                FOLLOW_KEY + safeUsername,
                new AlertToastText(
                        Translations.tr("reinodoce.alert.title.follow"),
                        Translations.tr("reinodoce.alert.follow", safeUsername)),
                new AlertToastEventData(
                        safeUsername,
                        "",
                        0,
                        0,
                        0,
                        new AlertMediaReferences(profileImageUrl, "")),
                DEFAULT_ALERT_COOLDOWN);
    }

    /**
     * Emits a join alert when enabled. Joins use a global cooldown because they can be high volume.
     *
     * @param config active configuration
     * @param username joining username
     */
    public void join(ReinodoceConfig config, String username) {
        join(config, username, "");
    }

    /**
     * Emits a join alert when enabled. Joins use a global cooldown because they can be high volume.
     *
     * @param config active configuration
     * @param username joining username
     * @param profileImageUrl joining user's profile image URL
     */
    public void join(ReinodoceConfig config, String username, String profileImageUrl) {
        if (!isEnabled(config, AlertEventType.JOIN)) {
            return;
        }
        String safeUsername = safe(username);
        emit(
                config,
                AlertEventType.JOIN,
                JOIN_KEY,
                new AlertToastText(
                        Translations.tr("reinodoce.alert.title.join"),
                        Translations.tr("reinodoce.alert.join", safeUsername)),
                new AlertToastEventData(
                        safeUsername,
                        "",
                        0,
                        0,
                        0,
                        new AlertMediaReferences(profileImageUrl, "")),
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
        memberLevel(config, username, memberLevel, "");
    }

    /**
     * Emits a member-level alert when enabled.
     *
     * @param config active configuration
     * @param username member username
     * @param memberLevel new member level
     * @param profileImageUrl member profile image URL
     */
    public void memberLevel(ReinodoceConfig config, String username, int memberLevel, String profileImageUrl) {
        if (!isEnabled(config, AlertEventType.MEMBER_LEVEL)) {
            return;
        }
        String safeUsername = safe(username);
        int safeLevel = Math.max(0, memberLevel);
        emit(
                config,
                AlertEventType.MEMBER_LEVEL,
                MEMBER_LEVEL_KEY + safeUsername + ':' + safeLevel,
                new AlertToastText(
                        Translations.tr("reinodoce.alert.title.member_level"),
                        Translations.tr("reinodoce.alert.member_level", safeUsername, safeLevel)),
                new AlertToastEventData(
                        safeUsername,
                        "",
                        0,
                        0,
                        safeLevel,
                        new AlertMediaReferences(profileImageUrl, "")),
                DEFAULT_ALERT_COOLDOWN);
    }

    private void emit(
            ReinodoceConfig config,
            AlertEventType eventType,
            String key,
            AlertToastText toastText,
            AlertToastEventData eventData,
            Duration cooldown
    ) {
        if (!shouldEmit(key, cooldown)) {
            return;
        }
        if (config.isAlertSoundEnabled(eventType)) {
            alertSink.playAlertSound(config.getAlertSoundId(eventType));
        }
        if (config.isAlertToastEnabled(eventType)) {
            alertSink.showAlertToast(payload(config, eventType, toastText, eventData));
        }
    }

    private static AlertToastPayload payload(
            ReinodoceConfig config,
            AlertEventType eventType,
            AlertToastText toastText,
            AlertToastEventData eventData
    ) {
        AlertToastMediaMode mediaMode = AlertToastMediaMode.fromString(config.getAlertMediaMode(eventType));
        String customImage = safe(config.getAlertCustomImage(eventType));
        Map<String, String> values = eventData.values();
        String profileImageUrl = values.getOrDefault("profileImage", "");
        String giftImageUrl = values.getOrDefault("giftImage", "");
        String renderedMessage = AlertToastTemplateRenderer.render(
                config.getAlertToastTemplate(eventType),
                values,
                toastText.message());
        return new AlertToastPayload(
                eventType,
                toastText.title(),
                renderedMessage,
                mediaMode,
                selectMediaSource(mediaMode, profileImageUrl, giftImageUrl, customImage),
                profileImageUrl,
                giftImageUrl,
                customImage);
    }

    private static String selectMediaSource(
            AlertToastMediaMode mediaMode,
            String profileImageUrl,
            String giftImageUrl,
            String customImage
    ) {
        return switch (mediaMode) {
            case PROFILE -> profileImageUrl;
            case CUSTOM -> customImage;
            case PROFILE_CUSTOM -> profileImageUrl.isBlank() ? customImage : profileImageUrl;
            case GIFT -> giftImageUrl;
            case INLINE -> firstNonBlank(profileImageUrl, giftImageUrl, customImage);
            case NONE -> "";
        };
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private static AlertMediaReferences media(AlertMediaReferences mediaReferences) {
        return mediaReferences == null ? AlertMediaReferences.EMPTY : mediaReferences;
    }

    private record AlertToastEventData(
            String username,
            String giftName,
            int count,
            long diamonds,
            int memberLevel,
            AlertMediaReferences mediaReferences
    ) {
        Map<String, String> values() {
            Map<String, String> values = new HashMap<>();
            values.put("username", safe(username));
            values.put("giftName", safe(giftName));
            values.put("count", count <= 0 ? "" : Integer.toString(count));
            values.put("diamonds", diamonds <= 0 ? "" : Long.toString(diamonds));
            values.put("memberLevel", memberLevel <= 0 ? "" : Integer.toString(memberLevel));
            AlertMediaReferences media = media(mediaReferences);
            values.put("profileImage", profileImage(media.profileImageUrl()));
            values.put("giftImage", safe(media.giftImageUrl()));
            return values;
        }
    }

    private record AlertToastText(String title, String message) {
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

    private static String profileImage(String value) {
        String safeValue = safe(value);
        return InlineMediaUrls.defaultAvatarUrl().equals(safeValue) ? "" : safeValue;
    }
}
