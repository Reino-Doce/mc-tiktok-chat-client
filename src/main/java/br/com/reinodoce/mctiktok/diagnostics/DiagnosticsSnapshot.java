package br.com.reinodoce.mctiktok.diagnostics;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.tiktok.SessionStatsTracker;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable inputs used to build a sanitized diagnostics report.
 *
 * @param config current runtime configuration
 * @param session current connection lifecycle snapshot
 * @param stats current session counters
 * @param effectiveLanguage resolved language used for TikTok requests and fixed text
 * @param configPath persisted config path
 * @param client client-side renderer and cache diagnostics
 */
@SuppressWarnings("PMD.DataClass")
public record DiagnosticsSnapshot(
        ReinodoceConfig config,
        LiveSessionState.Snapshot session,
        SessionStatsTracker.Snapshot stats,
        String effectiveLanguage,
        Path configPath,
        Map<String, Object> client
) {
    /**
     * Creates a diagnostics snapshot without client-specific diagnostics.
     *
     * @param config current runtime configuration
     * @param session current connection lifecycle snapshot
     * @param stats current session counters
     * @param effectiveLanguage resolved language used for TikTok requests and fixed text
     * @param configPath persisted config path
     */
    public DiagnosticsSnapshot(
            ReinodoceConfig config,
            LiveSessionState.Snapshot session,
            SessionStatsTracker.Snapshot stats,
            String effectiveLanguage,
            Path configPath
    ) {
        this(config, session, stats, effectiveLanguage, configPath, Map.of());
    }

    /**
     * Creates a diagnostics snapshot.
     */
    public DiagnosticsSnapshot {
        config = Objects.requireNonNull(config, "config");
        session = Objects.requireNonNull(session, "session");
        stats = Objects.requireNonNull(stats, "stats");
        effectiveLanguage = effectiveLanguage == null ? "" : effectiveLanguage;
        client = client == null ? Map.of() : Map.copyOf(client);
    }
}
