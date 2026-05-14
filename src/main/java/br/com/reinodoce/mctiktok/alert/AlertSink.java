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
     * Shows a local toast alert.
     *
     * @param title toast title
     * @param message toast message
     */
    void showAlertToast(String title, String message);

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
        public void showAlertToast(String title, String message) {
        }
    }
}
