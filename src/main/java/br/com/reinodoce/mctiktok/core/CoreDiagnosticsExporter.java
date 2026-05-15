package br.com.reinodoce.mctiktok.core;

import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.diagnostics.DiagnosticsExporter;
import br.com.reinodoce.mctiktok.diagnostics.DiagnosticsSanitizer;
import br.com.reinodoce.mctiktok.diagnostics.DiagnosticsSnapshot;
import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.tiktok.TikTokClientFacade;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Bridges core runtime state into sanitized support diagnostics files.
 */
final class CoreDiagnosticsExporter {
    private final DiagnosticsExporter diagnosticsExporter;

    CoreDiagnosticsExporter(Path sessionLogDirectory) {
        this.diagnosticsExporter = new DiagnosticsExporter(
                DiagnosticsExporter.defaultSupportDirectory(sessionLogDirectory));
    }

    CommandResult export(
            ReinodoceConfig config,
            TikTokClientFacade facade,
            String effectiveLanguage,
            Path configPath,
            Map<String, Object> clientDiagnostics
    ) {
        try {
            Path reportPath = diagnosticsExporter.export(new DiagnosticsSnapshot(
                    config,
                    facade.status(),
                    facade.stats(),
                    effectiveLanguage,
                    configPath,
                    clientDiagnostics));
            return CommandResult.ok(Translations.tr(
                    "reinodoce.command.diagnostics.export",
                    reportPath.getFileName()));
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.warn("Failed to export diagnostics", exception);
            return CommandResult.error(Translations.tr(
                    "reinodoce.command.diagnostics.export_failed",
                    DiagnosticsSanitizer.text(exception.getMessage())));
        }
    }
}
