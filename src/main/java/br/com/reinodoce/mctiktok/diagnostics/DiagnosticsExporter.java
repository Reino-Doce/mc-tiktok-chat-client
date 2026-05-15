package br.com.reinodoce.mctiktok.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Writes sanitized local support diagnostics.
 */
public final class DiagnosticsExporter {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final int FIRST_ROTATED_INDEX = 2;
    private static final String DIAGNOSTICS_DIRECTORY = "diagnostics";
    private static final String FILE_EXTENSION = ".json";
    private static final String FILE_PREFIX = "reinodoce-diagnostics-";

    private final Path supportDirectory;
    private final Clock clock;

    /**
     * Creates an exporter rooted at the local diagnostics support directory.
     *
     * @param supportDirectory diagnostics output directory
     */
    public DiagnosticsExporter(Path supportDirectory) {
        this(supportDirectory, Clock.systemUTC());
    }

    DiagnosticsExporter(Path supportDirectory, Clock clock) {
        this.supportDirectory = Objects.requireNonNull(supportDirectory, "supportDirectory");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Returns the default diagnostics directory under the ReinoDoce log directory.
     *
     * @param sessionLogDirectory existing ReinoDoce session log directory
     * @return diagnostics support directory
     */
    public static Path defaultSupportDirectory(Path sessionLogDirectory) {
        return Objects.requireNonNull(sessionLogDirectory, "sessionLogDirectory").resolve(DIAGNOSTICS_DIRECTORY);
    }

    /**
     * Writes a sanitized diagnostics report.
     *
     * @param snapshot report source data
     * @return created report path
     * @throws IOException when the report cannot be written
     */
    public Path export(DiagnosticsSnapshot snapshot) throws IOException {
        DiagnosticsSnapshot safeSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        Instant generatedAt = Instant.now(clock);
        Files.createDirectories(supportDirectory);
        Path reportPath = resolveReportPath(generatedAt);
        try (Writer writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8)) {
            GSON.toJson(DiagnosticsReportBuilder.build(generatedAt, supportDirectory, reportPath, safeSnapshot), writer);
        }
        return reportPath;
    }

    private Path resolveReportPath(Instant generatedAt) {
        String timestamp = FILE_TIMESTAMP_FORMAT.format(generatedAt);
        Path candidate = supportDirectory.resolve(FILE_PREFIX + timestamp + FILE_EXTENSION);
        if (!Files.exists(candidate)) {
            return candidate;
        }
        for (int index = FIRST_ROTATED_INDEX; ; index++) {
            candidate = supportDirectory.resolve(FILE_PREFIX + timestamp + "-" + index + FILE_EXTENSION);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }
}
