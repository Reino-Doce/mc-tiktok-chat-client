package br.com.reinodoce.mctiktok.diagnostics;

import br.com.reinodoce.mctiktok.ReinodoceMcTiktokMod;

import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the top-level sanitized diagnostics report.
 */
final class DiagnosticsReportBuilder {
    private static final DateTimeFormatter REPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;
    private static final int SCHEMA_VERSION = 1;

    private DiagnosticsReportBuilder() {
    }

    static Map<String, Object> build(
            Instant generatedAt,
            Path supportDirectory,
            Path reportPath,
            DiagnosticsSnapshot snapshot
    ) {
        Collection<String> knownUsernames = knownUsernames(snapshot);
        Map<String, Object> report = ordered();
        report.put("schemaVersion", SCHEMA_VERSION);
        report.put("generatedAt", REPORT_TIMESTAMP_FORMAT.format(generatedAt));
        report.put("privacy", privacy());
        report.put("mod", mod());
        report.put("environment", environment());
        report.put("paths", paths(snapshot.configPath(), supportDirectory, reportPath));
        report.put("client", DiagnosticsClientReport.from(snapshot.client()));
        report.put("connection", connection(snapshot));
        report.put("config", DiagnosticsConfigReport.from(
                snapshot.config(),
                snapshot.effectiveLanguage(),
                knownUsernames));
        report.put("stats", stats(snapshot.stats()));
        return report;
    }

    private static Collection<String> knownUsernames(DiagnosticsSnapshot snapshot) {
        List<String> usernames = new ArrayList<>();
        usernames.add(snapshot.config().getLastUsername());
        usernames.add(snapshot.session().username());
        usernames.add(snapshot.stats().topGifter());
        usernames.addAll(snapshot.config().getRuleBlockedUsers());
        return usernames;
    }

    private static Map<String, Object> privacy() {
        Map<String, Object> privacy = ordered();
        privacy.put("rawChatMessagesIncluded", false);
        privacy.put("sessionLogContentIncluded", false);
        privacy.put("credentialsIncluded", false);
        privacy.put("cookiesIncluded", false);
        privacy.put("usernames", "redacted");
        privacy.put("localHomePaths", "redacted");
        return privacy;
    }

    private static Map<String, Object> mod() {
        Map<String, Object> mod = ordered();
        mod.put("modId", ReinodoceMcTiktokMod.MOD_ID);
        mod.put("implementationVersion", DiagnosticsSanitizer.text(implementationVersion()));
        return mod;
    }

    private static String implementationVersion() {
        String version = ReinodoceMcTiktokMod.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "dev" : version;
    }

    private static Map<String, Object> environment() {
        Map<String, Object> environment = ordered();
        environment.put("side", "client");
        environment.put("javaVersion", DiagnosticsSanitizer.text(System.getProperty("java.version")));
        environment.put("javaVendor", DiagnosticsSanitizer.text(System.getProperty("java.vendor")));
        environment.put("osName", DiagnosticsSanitizer.text(System.getProperty("os.name")));
        environment.put("osVersion", DiagnosticsSanitizer.text(System.getProperty("os.version")));
        environment.put("osArch", DiagnosticsSanitizer.text(System.getProperty("os.arch")));
        return environment;
    }

    private static Map<String, Object> paths(Path configPath, Path diagnosticsDirectory, Path reportPath) {
        Map<String, Object> paths = ordered();
        paths.put("configFile", DiagnosticsSanitizer.path(configPath));
        paths.put("diagnosticsDirectory", DiagnosticsSanitizer.path(diagnosticsDirectory));
        paths.put("reportFile", DiagnosticsSanitizer.path(reportPath));
        return paths;
    }

    private static Map<String, Object> connection(DiagnosticsSnapshot snapshot) {
        Map<String, Object> connection = ordered();
        connection.put("state", snapshot.session().state().name());
        connection.put("username", DiagnosticsSanitizer.username(snapshot.session().username()));
        connection.put("lastErrorPresent", !snapshot.session().lastError().isBlank());
        connection.put("lastError", snapshot.session().lastError().isBlank() ? "" : DiagnosticsSanitizer.REDACTED);
        connection.put("reconnectAt", reconnectAt(snapshot));
        connection.put("reconnectAttempts", snapshot.session().reconnectAttempts());
        return connection;
    }

    private static String reconnectAt(DiagnosticsSnapshot snapshot) {
        Instant reconnectAt = snapshot.session().reconnectAt();
        return reconnectAt == null ? "" : REPORT_TIMESTAMP_FORMAT.format(reconnectAt);
    }

    private static Map<String, Object> stats(br.com.reinodoce.mctiktok.tiktok.SessionStatsTracker.Snapshot stats) {
        Map<String, Object> output = ordered();
        output.put("messages", stats.messages());
        output.put("uniqueChatters", stats.uniqueChatters());
        output.put("follows", stats.follows());
        output.put("joins", stats.joins());
        output.put("gifts", stats.gifts());
        output.put("diamonds", stats.diamonds());
        output.put("memberLevels", stats.memberLevels());
        output.put("topGifter", DiagnosticsSanitizer.username(stats.topGifter()));
        output.put("topGifterDiamonds", stats.topGifterDiamonds());
        return output;
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
