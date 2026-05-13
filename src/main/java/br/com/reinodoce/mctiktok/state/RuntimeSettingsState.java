package br.com.reinodoce.mctiktok.state;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe holder for runtime configuration snapshots.
 */
public class RuntimeSettingsState {
    private final AtomicReference<ReinodoceConfig> configRef = new AtomicReference<>(ReinodoceConfig.defaults());

    /**
     * Returns a defensive copy of the current runtime configuration.
     *
     * @return current configuration snapshot
     */
    public ReinodoceConfig getSnapshot() {
        return configRef.get().copy();
    }

    /**
     * Replaces the runtime configuration with a defensive copy.
     *
     * @param config configuration to store
     */
    public void set(ReinodoceConfig config) {
        configRef.set(config.copy());
    }
}
