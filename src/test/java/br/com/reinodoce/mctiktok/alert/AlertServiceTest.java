package br.com.reinodoce.mctiktok.alert;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertServiceTest {
    private static final Instant START = Instant.parse("2026-05-14T00:00:00Z");
    private static final String ALICE = "alice";
    private static final String ROSE = "Rose";
    private static final String CUSTOM_SOUND_ID = "minecraft:entity.experience_orb.pickup";
    private static final String PROFILE_IMAGE = "https://cdn.example/alice.png";
    private static final String GIFT_IMAGE = "https://cdn.example/rose.png";

    @Test
    void defaultsDoNotEmitAlerts() {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));

        service.gift(ReinodoceConfig.defaults(), ALICE, ROSE, 1, 1);
        service.follow(ReinodoceConfig.defaults(), ALICE);
        service.join(ReinodoceConfig.defaults(), ALICE);
        service.memberLevel(ReinodoceConfig.defaults(), ALICE, 2);

        assertEquals(0, sink.sounds());
        assertEquals(0, sink.toasts());
    }

    @Test
    void giftAlertRequiresConfiguredMinimumValue() {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setAlertSoundEnabled(AlertEventType.GIFT, true);
        config.setAlertToastEnabled(AlertEventType.GIFT, true);
        config.setAlertGiftMinValue(100);

        service.gift(config, ALICE, ROSE, 1, 1);
        service.gift(config, ALICE, "Galaxy", 2, 100);

        assertEquals(1, sink.sounds());
        assertEquals(1, sink.toasts());
        assertTrue(sink.message().contains("Galaxy") || sink.message().contains("reinodoce.alert.gift"));
    }

    @Test
    void soundOnlyAlertsEmitWithoutToast() {
        assertSoundOnlyAlert(AlertEventType.GIFT);
        assertSoundOnlyAlert(AlertEventType.FOLLOW);
        assertSoundOnlyAlert(AlertEventType.JOIN);
        assertSoundOnlyAlert(AlertEventType.MEMBER_LEVEL);
    }

    @Test
    void soundAlertsPassEventSpecificSoundIdOnlyWhenSurfaced() {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setAlertSoundEnabled(AlertEventType.GIFT, true);
        config.setAlertSoundId(AlertEventType.GIFT, CUSTOM_SOUND_ID);
        config.setAlertGiftMinValue(100);

        service.gift(config, ALICE, ROSE, 1, 1);
        service.gift(config, ALICE, "Galaxy", 1, 100);

        assertEquals(List.of(CUSTOM_SOUND_ID), sink.soundIds());
        assertEquals(0, sink.toasts());
    }

    @Test
    void toastOnlyAlertsKeepExistingToastPath() {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setAlertToastEnabled(AlertEventType.FOLLOW, true);

        service.follow(config, ALICE);

        assertEquals(0, sink.sounds());
        assertEquals(1, sink.toasts());
    }

    @Test
    void toastTemplateRendersEventTokensAndMediaPayload() {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setAlertToastEnabled(AlertEventType.GIFT, true);
        config.setAlertToastTemplate(AlertEventType.GIFT, "{username} gave {giftName} x{count} for {diamonds}");
        config.setAlertMediaMode(AlertEventType.GIFT, "gift");

        service.gift(config, ALICE, ROSE, 3, 15, new AlertMediaReferences(PROFILE_IMAGE, GIFT_IMAGE));

        assertEquals("alice gave Rose x3 for 45", sink.message());
        assertEquals(AlertToastMediaMode.GIFT, sink.payload().mediaMode());
        assertEquals(GIFT_IMAGE, sink.payload().mediaSource());
        assertEquals(PROFILE_IMAGE, sink.payload().profileImageUrl());
    }

    @Test
    void missingMediaFallsBackToTextOnlyPayloadWithoutThrowing() {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setAlertToastEnabled(AlertEventType.FOLLOW, true);
        config.setAlertMediaMode(AlertEventType.FOLLOW, "profile");

        service.follow(config, ALICE, InlineMediaUrls.defaultAvatarUrl());

        assertEquals(1, sink.toasts());
        assertEquals(AlertToastMediaMode.PROFILE, sink.payload().mediaMode());
        assertEquals("", sink.payload().mediaSource());
        assertEquals("", sink.payload().profileImageUrl());
        assertTrue(sink.message().contains(ALICE) || sink.message().contains("reinodoce.alert.follow"));
    }

    @Test
    void profileCustomModeFallsBackToCustomImageWhenProfileIsOnlyDefaultAvatar() {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setAlertToastEnabled(AlertEventType.FOLLOW, true);
        config.setAlertMediaMode(AlertEventType.FOLLOW, "profile-custom");
        config.setAlertCustomImage(AlertEventType.FOLLOW, "reinodoce_mctiktok:textures/gui/no_user_image.png");

        service.follow(config, ALICE, InlineMediaUrls.defaultAvatarUrl());

        assertEquals(config.getAlertCustomImage(AlertEventType.FOLLOW), sink.payload().mediaSource());
        assertEquals("", sink.payload().profileImageUrl());
    }

    @Test
    void joinAlertsUseGlobalCooldown() {
        RecordingAlertSink sink = new RecordingAlertSink();
        MutableClock clock = new MutableClock(START);
        AlertService service = new AlertService(sink, clock);
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setAlertToastEnabled(AlertEventType.JOIN, true);

        service.join(config, ALICE);
        service.join(config, "bob");
        clock.advanceSeconds(10);
        service.join(config, "carol");

        assertEquals(2, sink.toasts());
    }

    private static void emit(AlertService service, ReinodoceConfig config, AlertEventType eventType) {
        switch (eventType) {
            case GIFT -> service.gift(config, ALICE, ROSE, 1, 1);
            case FOLLOW -> service.follow(config, ALICE);
            case JOIN -> service.join(config, ALICE);
            case MEMBER_LEVEL -> service.memberLevel(config, ALICE, 2);
            default -> throw new IllegalArgumentException(eventType.id());
        }
    }

    private static void assertSoundOnlyAlert(AlertEventType eventType) {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setAlertSoundEnabled(eventType, true);

        emit(service, config, eventType);

        assertEquals(1, sink.sounds(), eventType.id());
        assertEquals(0, sink.toasts(), eventType.id());
    }

    private static final class RecordingAlertSink implements AlertSink {
        private int recordedSounds;
        private int recordedToasts;
        private String recordedMessage = "";
        private AlertToastPayload recordedPayload;
        private final List<String> recordedSoundIds = new ArrayList<>();

        @Override
        public void playAlertSound() {
            recordedSounds++;
        }

        @Override
        public void playAlertSound(String soundId) {
            recordedSounds++;
            recordedSoundIds.add(soundId);
        }

        @Override
        public void showAlertToast(String title, String message) {
            recordedToasts++;
            recordedMessage = message;
        }

        @Override
        public void showAlertToast(AlertToastPayload payload) {
            recordedToasts++;
            recordedPayload = payload;
            recordedMessage = payload.message();
        }

        int sounds() {
            return recordedSounds;
        }

        int toasts() {
            return recordedToasts;
        }

        String message() {
            return recordedMessage;
        }

        AlertToastPayload payload() {
            return recordedPayload;
        }

        List<String> soundIds() {
            return List.copyOf(recordedSoundIds);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant currentInstant;

        private MutableClock(Instant instant) {
            this.currentInstant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        private void advanceSeconds(long seconds) {
            currentInstant = currentInstant.plusSeconds(seconds);
        }
    }
}
