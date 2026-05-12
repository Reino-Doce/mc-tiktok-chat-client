package br.com.reinodoce.mctiktok.config;

import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ReinodoceConfigRepository {
    public static final String DEFAULT_FILE_NAME = "reinodoce-mc-tiktok-client.json";
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final Path storedConfigPath;

    public ReinodoceConfigRepository() {
        this(Paths.get("config").resolve(DEFAULT_FILE_NAME));
    }

    public ReinodoceConfigRepository(Path configPath) {
        this.storedConfigPath = Objects.requireNonNull(configPath, "configPath");
    }

    public Path configPath() {
        return storedConfigPath;
    }

    public ReinodoceConfig load() {
        if (!Files.exists(storedConfigPath)) {
            return ReinodoceConfig.defaults();
        }
        try (Reader reader = Files.newBufferedReader(storedConfigPath)) {
            return sanitize(GSON.fromJson(reader, ReinodoceConfig.class));
        } catch (IOException | JsonIOException | JsonSyntaxException exception) {
            ReinodoceLogger.LOGGER.warn("Failed to load config from {}", storedConfigPath, exception);
            return ReinodoceConfig.defaults();
        }
    }

    public void save(ReinodoceConfig config) {
        ReinodoceConfig safeConfig = sanitize(config);
        Path parent = storedConfigPath.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(storedConfigPath)) {
                GSON.toJson(safeConfig, writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save config to " + storedConfigPath, exception);
        }
    }

    private ReinodoceConfig sanitize(ReinodoceConfig config) {
        return config == null ? ReinodoceConfig.defaults() : config.copy();
    }
}
