package br.com.reinodoce.mctiktok.diagnostics;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.state.ConnectionLifecycleState;
import br.com.reinodoce.mctiktok.state.LiveSessionState;
import br.com.reinodoce.mctiktok.tiktok.SessionStatsTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticsExporterTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-15T12:00:00Z");
    private static final String RAW_CHAT_TEXT = "session-log-raw-chat";
    private static final String STREAMER = "SecretStreamer";
    private static final String BLOCKED_USER = "BlockedUser";
    private static final String TOP_GIFTER = "TopGifter";

    @TempDir
    Path tempDir;

    @Test
    void exportWritesPredictableSanitizedReport() throws IOException {
        Path logDirectory = tempDir.resolve("logs").resolve("reinodoce");
        Path supportDirectory = logDirectory.resolve("diagnostics");
        Files.createDirectories(logDirectory);
        Files.writeString(logDirectory.resolve("live-existing.jsonl"), RAW_CHAT_TEXT);
        DiagnosticsExporter exporter = new DiagnosticsExporter(
                supportDirectory,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        Path reportPath = exporter.export(snapshot());
        String report = Files.readString(reportPath);

        assertEquals(supportDirectory, reportPath.getParent());
        assertEquals("reinodoce-diagnostics-20260515-120000.json", reportPath.getFileName().toString());
        assertTrue(report.contains("\"rawChatMessagesIncluded\": false"));
        assertTrue(report.contains("\"sessionLogContentIncluded\": false"));
        assertTrue(report.contains(DiagnosticsSanitizer.REDACTED_USERNAME));
        assertTrue(report.contains("https://cdn.example/avatar.png?<redacted>"));
        assertFalse(report.contains(STREAMER));
        assertFalse(report.contains(BLOCKED_USER));
        assertFalse(report.contains(TOP_GIFTER));
        assertFalse(report.contains(RAW_CHAT_TEXT));
        assertFalse(report.contains("hunter2"));
        assertFalse(report.contains("abc123"));
        assertFalse(report.contains("accessValue"));
        assertFalse(report.contains("refreshValue"));
        assertFalse(report.contains("clientValue"));
        assertFalse(report.contains("sessionValue"));
        assertFalse(report.contains("apiValue"));
        assertFalse(report.contains("sessionid"));
        assertFalse(report.contains("C:\\Users\\schim"));
        assertFalse(report.contains("schim"));
    }

    @Test
    void sanitizerRedactsKnownBareUsernamesAndSecrets() {
        String sanitized = DiagnosticsSanitizer.text(
                "Failed for SecretStreamer and @OtherUser with Bearer abc123 access_token=accessValue "
                        + "refresh_token=refreshValue client_secret=clientValue session_id=sessionValue "
                        + "api-key=apiValue at C:\\Users\\schim\\.minecraft",
                List.of(STREAMER));

        assertFalse(sanitized.contains(STREAMER));
        assertFalse(sanitized.contains("OtherUser"));
        assertFalse(sanitized.contains("abc123"));
        assertFalse(sanitized.contains("accessValue"));
        assertFalse(sanitized.contains("refreshValue"));
        assertFalse(sanitized.contains("clientValue"));
        assertFalse(sanitized.contains("sessionValue"));
        assertFalse(sanitized.contains("apiValue"));
        assertFalse(sanitized.contains("schim"));
        assertTrue(sanitized.contains(DiagnosticsSanitizer.REDACTED_USERNAME));
        assertTrue(sanitized.contains(DiagnosticsSanitizer.REDACTED));
    }

    private static DiagnosticsSnapshot snapshot() {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setLastUsername(STREAMER);
        config.setRuleBlockedUsers(List.of(BLOCKED_USER));
        config.setRuleBlockedWords(List.of("raw-chat-fragment"));
        config.setChatPrefix("prefix token=abc123");
        config.setChatFormat("{prefix} <{username}> password=hunter2 {message}");
        config.setSessionLoggingEnabled(true);
        config.setAlertCustomImage(
                AlertEventType.GIFT,
                "https://cdn.example/avatar.png?token=abc123&cookie=sessionid");
        LiveSessionState.Snapshot session = new LiveSessionState.Snapshot(
                ConnectionLifecycleState.CONNECTED,
                STREAMER,
                "Raw chat said " + RAW_CHAT_TEXT + " for @" + BLOCKED_USER + " token=abc123",
                FIXED_INSTANT,
                2);
        SessionStatsTracker.Snapshot stats = new SessionStatsTracker.Snapshot(
                5L, 2, 1L, 1L, 3L, 30L, 1L, TOP_GIFTER, 30L);
        return new DiagnosticsSnapshot(
                config,
                session,
                stats,
                "en_us",
                Path.of("C:\\Users\\schim\\.minecraft\\config\\reinodoce.json"),
                Map.of(
                        "inlineMediaRenderer", "font-coremod",
                        "access_token", "accessValue",
                        "nested", Map.of(
                                "safeCounter", 1,
                                "unsafeText", "refresh_token=refreshValue client_secret=clientValue",
                                "session_id", "sessionValue",
                                "api-key", "apiValue")));
    }
}
