package br.com.reinodoce.mctiktok.alert;

/**
 * Platform boundary for local-only alert effects.
 */
public interface AlertSink {
    /**
     * Returns a sink that drops all alert effects.
     *
     * @return no-op alert sink
     */
    static AlertSink noop() {
        return NoopAlertSink.INSTANCE;
    }

    /**
     * Plays the configured local alert sound.
     */
    void playAlertSound();

    /**
     * Plays the configured local alert sound.
     *
     * @param soundId persisted alert sound identifier
     */
    default void playAlertSound(String soundId) {
        playAlertSound();
    }

    /**
     * Shows a local toast alert.
     *
     * @param title toast title
     * @param message toast message
     */
    void showAlertToast(String title, String message);

    /**
     * Shows a local toast alert with optional media hints.
     *
     * @param payload rendered toast payload
     */
    default void showAlertToast(AlertToastPayload payload) {
        if (payload != null) {
            showAlertToast(payload.title(), payload.message());
        }
    }

    /**
     * No-op sink used by tests and non-Minecraft contexts.
     */
    final class NoopAlertSink implements AlertSink {
        private static final AlertSink INSTANCE = new NoopAlertSink();

        private NoopAlertSink() {
        }

        @Override
        public void playAlertSound() {
        }

        @Override
        public void playAlertSound(String soundId) {
        }

        @Override
        public void showAlertToast(String title, String message) {
        }

        @Override
        public void showAlertToast(AlertToastPayload payload) {
        }
    }
}
