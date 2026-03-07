package br.com.reinodoce.mctiktok.state;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

import java.util.concurrent.atomic.AtomicReference;

public class RuntimeSettingsState {
    private final AtomicReference<ReinodoceConfig> configRef = new AtomicReference<>(ReinodoceConfig.defaults());

    public ReinodoceConfig getSnapshot() {
        return configRef.get().copy();
    }

    public void set(ReinodoceConfig config) {
        configRef.set(config.copy());
    }
}
