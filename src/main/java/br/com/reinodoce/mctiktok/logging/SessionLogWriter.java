package br.com.reinodoce.mctiktok.logging;

import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Worker-thread state for one active session log file.
 */
final class SessionLogWriter {
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC);
    private static final int FIRST_ROTATED_INDEX = 2;

    private final Path logDirectory;
    private final Clock clock;

    private BufferedWriter writer;
    private SessionLogFormat writerFormat;
    private int writerGeneration;

    SessionLogWriter(Path logDirectory, Clock clock) {
        this.logDirectory = logDirectory;
        this.clock = clock;
    }

    boolean start(int generation, SessionLogFormat format, String username) {
        closeQuietly();
        try {
            Files.createDirectories(logDirectory);
            Path sessionFile = resolveSessionFile(format);
            writer = Files.newBufferedWriter(
                    sessionFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            writerFormat = format;
            writerGeneration = generation;
            ReinodoceLogger.LOGGER.info("TikTok LIVE session logging started for @{} at {}", username, sessionFile);
            return true;
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.warn("Failed to start TikTok LIVE session logging", exception);
            return false;
        }
    }

    boolean write(int generation, Instant timestamp, SessionLogEvent event) {
        if (writer == null || writerGeneration != generation) {
            return true;
        }
        try {
            writer.write(SessionLogFormatter.format(writerFormat, timestamp, event));
            writer.newLine();
            writer.flush();
            return true;
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.warn("Failed to write TikTok LIVE session log event", exception);
            closeQuietly();
            return false;
        }
    }

    void closeQuietly() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to close TikTok LIVE session log", exception);
        } finally {
            writer = null;
            writerFormat = null;
            writerGeneration = 0;
        }
    }

    private Path resolveSessionFile(SessionLogFormat format) {
        String timestamp = FILE_TIMESTAMP_FORMAT.format(Instant.now(clock));
        Path candidate = logDirectory.resolve("live-" + timestamp + format.extension());
        if (!Files.exists(candidate)) {
            return candidate;
        }
        for (int index = FIRST_ROTATED_INDEX; ; index++) {
            candidate = logDirectory.resolve("live-" + timestamp + "-" + index + format.extension());
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }
}
