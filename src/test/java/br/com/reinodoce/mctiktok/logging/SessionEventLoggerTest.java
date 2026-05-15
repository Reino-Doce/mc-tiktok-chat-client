package br.com.reinodoce.mctiktok.logging;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionEventLoggerTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-13T20:30:00Z");
    private static final String STREAMER = "streamer";
    private static final String VIEWER = "viewer";

    @TempDir
    Path tempDir;

    @Test
    void writesJsonlEventsToOneFilePerEnabledSession() throws Exception {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setSessionLoggingEnabled(true);
        try (SessionEventLogger logger = logger()) {
            logger.startSession(config, STREAMER);
            logger.log(SessionLogEvent.chat(VIEWER, "hello", 1));
            logger.stopSession();
            logger.awaitIdle();

            List<Path> files = files();
            assertEquals(1, files.size());
            assertTrue(files.get(0).getFileName().toString().endsWith(".jsonl"));
            String json = Files.readString(files.get(0));
            assertTrue(json.contains("\"type\":\"chat\""));
            assertTrue(json.contains("\"username\":\"" + VIEWER + "\""));
            assertTrue(json.contains("\"message\":\"hello\""));
            assertTrue(json.contains("\"memberLevel\":1"));
        }
    }

    @Test
    void textFormatEscapesLinesAndRotatesDuplicateSessionNames() throws Exception {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setSessionLoggingEnabled(true);
        config.setSessionLoggingFormat("text");
        try (SessionEventLogger logger = logger()) {
            logger.startSession(config, STREAMER);
            logger.log(SessionLogEvent.gift(VIEWER, "Rose", 3, 30));
            logger.stopSession();
            logger.startSession(config, STREAMER);
            logger.log(SessionLogEvent.join("viewer\nname"));
            logger.stopSession();
            logger.awaitIdle();

            List<Path> files = sortedFiles();
            assertEquals(2, files.size());
            assertTrue(files.stream().allMatch(path -> path.getFileName().toString().endsWith(".txt")));
            assertTrue(files.stream().anyMatch(path -> path.getFileName().toString().contains("-2.txt")));
            String contents = Files.readString(files.get(0)) + Files.readString(files.get(1));
            assertTrue(contents.contains("type=gift"));
            assertTrue(contents.contains("giftName=Rose"));
            assertTrue(contents.contains("diamonds=30"));
            assertTrue(contents.contains("username=viewer\\nname"));
        }
    }

    @Test
    void disabledLoggingDoesNotCreateAFile() throws Exception {
        try (SessionEventLogger logger = logger()) {
            logger.startSession(ReinodoceConfig.defaults(), STREAMER);
            logger.log(SessionLogEvent.follow("viewer"));
            logger.awaitIdle();

            assertTrue(files().isEmpty());
        }
    }

    @Test
    void anonymizedMetadataOnlyLoggingMasksUsernamesAndOmitsMessages() throws Exception {
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setSessionLoggingEnabled(true);
        config.setSessionLoggingAnonymized(true);
        config.setSessionLoggingMetadataOnly(true);
        try (SessionEventLogger logger = logger()) {
            logger.startSession(config, STREAMER);
            logger.log(SessionLogEvent.chat(VIEWER, "private message", 2));
            logger.log(SessionLogEvent.gift("gifter", "Rose", 3, 30));
            logger.stopSession();
            logger.awaitIdle();

            String json = Files.readString(files().get(0));
            assertTrue(json.contains("\"username\":\"<redacted-username>\""));
            assertTrue(json.contains("\"memberLevel\":2"));
            assertTrue(json.contains("\"giftName\":\"Rose\""));
            assertTrue(json.contains("\"count\":3"));
            assertTrue(json.contains("\"diamonds\":30"));
            assertFalse(json.contains(VIEWER));
            assertFalse(json.contains("gifter"));
            assertFalse(json.contains("private message"));
            assertFalse(json.contains("\"message\""));
        }
    }

    @Test
    void retentionCleanupOnlyDeletesOwnSessionLogFiles() throws Exception {
        Path oldJson = sessionFile("live-old.jsonl", FIXED_INSTANT.minusSeconds(172_800));
        Path oldText = sessionFile("live-old.txt", FIXED_INSTANT.minusSeconds(172_700));
        Path newerJson = sessionFile("live-new.jsonl", FIXED_INSTANT.minusSeconds(60));
        Path unrelated = sessionFile("notes.txt", FIXED_INSTANT.minusSeconds(172_800));
        Path wrongPrefix = sessionFile("other-live.jsonl", FIXED_INSTANT.minusSeconds(172_800));
        Files.createDirectories(tempDir.resolve("diagnostics"));
        Path diagnostic = tempDir.resolve("diagnostics").resolve("live-old.jsonl");
        Files.writeString(diagnostic, "diagnostic");
        Files.setLastModifiedTime(diagnostic, FileTime.from(FIXED_INSTANT.minusSeconds(172_800)));
        ReinodoceConfig config = ReinodoceConfig.defaults();
        config.setSessionLoggingEnabled(true);
        config.setSessionLoggingRetentionDays(1);
        config.setSessionLoggingRetentionFiles(2);

        try (SessionEventLogger logger = logger()) {
            logger.startSession(config, STREAMER);
            logger.stopSession();
            logger.awaitIdle();
        }

        assertFalse(Files.exists(oldJson));
        assertFalse(Files.exists(oldText));
        assertTrue(Files.exists(newerJson));
        assertTrue(Files.exists(unrelated));
        assertTrue(Files.exists(wrongPrefix));
        assertTrue(Files.exists(diagnostic));
        assertEquals(2, files().stream()
                .filter(path -> path.getFileName().toString().startsWith("live-"))
                .filter(path -> path.getFileName().toString().endsWith(".jsonl")
                        || path.getFileName().toString().endsWith(".txt"))
                .count());
    }

    private SessionEventLogger logger() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return new SessionEventLogger(tempDir, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC), executor);
    }

    private List<Path> files() throws IOException {
        try (Stream<Path> stream = Files.list(tempDir)) {
            return stream.toList();
        }
    }

    private List<Path> sortedFiles() throws IOException {
        try (Stream<Path> stream = Files.list(tempDir)) {
            return stream
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted()
                    .map(tempDir::resolve)
                    .toList();
        }
    }

    private Path sessionFile(String fileName, Instant modified) throws IOException {
        Path path = tempDir.resolve(fileName);
        Files.writeString(path, fileName);
        Files.setLastModifiedTime(path, FileTime.from(modified));
        return path;
    }
}
