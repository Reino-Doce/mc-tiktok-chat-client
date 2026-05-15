package br.com.reinodoce.mctiktok.logging;

import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Deletes old session log files inside the mod-owned session log directory.
 */
final class SessionLogRetentionCleaner {
    private static final String LIVE_LOG_PREFIX = "live-";
    private static final Set<String> SESSION_LOG_EXTENSIONS = Set.of(".jsonl", ".txt");

    private SessionLogRetentionCleaner() {
    }

    static void clean(Path logDirectory, Clock clock, SessionLogPrivacyOptions options, Path preservedFile) {
        if (logDirectory == null || options == null || options.retentionDays() == 0 && options.retentionFiles() == 0) {
            return;
        }
        try {
            Files.createDirectories(logDirectory);
            deleteExpiredFiles(logDirectory, clock, options.retentionDays(), preservedFile);
            trimRetainedFiles(logDirectory, options.retentionFiles(), preservedFile);
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to clean TikTok LIVE session logs in {}", logDirectory, exception);
        }
    }

    private static void deleteExpiredFiles(
            Path logDirectory, Clock clock, int retentionDays, Path preservedFile
    ) throws IOException {
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now(clock).minus(Duration.ofDays(retentionDays));
        for (SessionLogFile file : sessionLogFiles(logDirectory)) {
            if (!sameFile(file.path(), preservedFile) && file.modifiedTime().toInstant().isBefore(cutoff)) {
                deleteQuietly(file.path());
            }
        }
    }

    private static void trimRetainedFiles(Path logDirectory, int retentionFiles, Path preservedFile) throws IOException {
        if (retentionFiles <= 0) {
            return;
        }
        List<SessionLogFile> files = sessionLogFiles(logDirectory).stream()
                .filter(file -> !sameFile(file.path(), preservedFile))
                .sorted(Comparator.comparing(SessionLogFile::modifiedTime).reversed())
                .toList();
        int oldFilesToKeep = Math.max(0, retentionFiles - 1);
        for (int index = oldFilesToKeep; index < files.size(); index++) {
            deleteQuietly(files.get(index).path());
        }
    }

    private static List<SessionLogFile> sessionLogFiles(Path logDirectory) throws IOException {
        try (Stream<Path> paths = Files.list(logDirectory)) {
            return paths
                    .filter(SessionLogRetentionCleaner::isSessionLogFile)
                    .map(SessionLogRetentionCleaner::toSessionLogFile)
                    .flatMap(List::stream)
                    .toList();
        }
    }

    private static boolean isSessionLogFile(Path path) {
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)) {
            return false;
        }
        String fileName = path.getFileName().toString();
        return fileName.startsWith(LIVE_LOG_PREFIX) && hasSessionLogExtension(fileName);
    }

    private static boolean hasSessionLogExtension(String fileName) {
        return SESSION_LOG_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private static List<SessionLogFile> toSessionLogFile(Path path) {
        try {
            return List.of(new SessionLogFile(path, Files.getLastModifiedTime(path)));
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to inspect TikTok LIVE session log {}", path, exception);
            return List.of();
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            ReinodoceLogger.LOGGER.debug("Failed to delete TikTok LIVE session log {}", path, exception);
        }
    }

    private static boolean sameFile(Path left, Path right) {
        return right != null && left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }

    private record SessionLogFile(Path path, FileTime modifiedTime) {
    }
}
