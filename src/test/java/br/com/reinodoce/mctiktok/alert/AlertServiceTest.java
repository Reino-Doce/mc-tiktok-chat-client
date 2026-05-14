package br.com.reinodoce.mctiktok.alert;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertServiceTest {
    private static final Instant START = Instant.parse("2026-05-14T00:00:00Z");
    private static final String ALICE = "alice";

    @Test
    void defaultsDoNotEmitAlerts() {
        RecordingAlertSink sink = new RecordingAlertSink();
        AlertService service = new AlertService(sink, new MutableClock(START));

        service.gift(ReinodoceConfig.defaults(), ALICE, "Rose", 1, 1);
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

        service.gift(config, ALICE, "Rose", 1, 1);
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
            case GIFT -> service.gift(config, ALICE, "Rose", 1, 1);
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

        @Override
        public void playAlertSound() {
            recordedSounds++;
        }

        @Override
        public void showAlertToast(String title, String message) {
            recordedToasts++;
            recordedMessage = message;
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
