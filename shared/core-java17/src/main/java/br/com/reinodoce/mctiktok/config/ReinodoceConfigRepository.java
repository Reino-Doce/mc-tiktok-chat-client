package br.com.reinodoce.mctiktok.config;

import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ReinodoceConfigRepository {
    private static final String FILE_NAME = "reinodoce-mc-tiktok-client.json";

    private final Path configFile;
    private final Gson gson;

    public ReinodoceConfigRepository(Path configDir) {
        this.configFile = configDir.resolve(FILE_NAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public ReinodoceConfig load() {
        if (!Files.exists(configFile)) {
            return ReinodoceConfig.defaults();
        }

        try {
            String json = Files.readString(configFile, StandardCharsets.UTF_8);
            ReinodoceConfig loaded = gson.fromJson(json, ReinodoceConfig.class);
            return sanitize(loaded);
        } catch (Exception exception) {
            ReinodoceLogger.LOGGER.error("Failed to load config at {}", configFile, exception);
            return ReinodoceConfig.defaults();
        }
    }

    public void save(ReinodoceConfig config) {
        ReinodoceConfig sanitized = sanitize(config);
        try {
            Files.createDirectories(configFile.getParent());
            Path tempFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");
            Files.writeString(tempFile, gson.toJson(sanitized), StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveException) {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            ReinodoceLogger.LOGGER.error("Failed to save config at {}", configFile, exception);
        }
    }

    private ReinodoceConfig sanitize(ReinodoceConfig incoming) {
        ReinodoceConfig defaults = ReinodoceConfig.defaults();
        if (incoming == null) {
            return defaults;
        }

        ReinodoceConfig sanitized = defaults.copy();
        sanitized.setLastUsername(nullToEmpty(incoming.getLastUsername()));
        sanitized.setReconnectSeconds(Math.max(0, incoming.getReconnectSeconds()));
        sanitized.setRuleFollowerOnly(incoming.isRuleFollowerOnly());
        sanitized.setRuleMinMemberLevel(Math.max(0, incoming.getRuleMinMemberLevel()));
        sanitized.setSynteticGiftMinValue(Math.max(0, incoming.getSynteticGiftMinValue()));
        sanitized.setSynteticGiftComboMode(incoming.getSynteticGiftComboMode());
        sanitized.setSynteticFollowEnabled(incoming.isSynteticFollowEnabled());
        sanitized.setSynteticJoinEnabled(incoming.isSynteticJoinEnabled());
        sanitized.setSynteticMemberLevelEnabled(incoming.isSynteticMemberLevelEnabled());
        sanitized.setChatEmotesEnabled(incoming.isChatEmotesEnabled());
        String prefix = nullToEmpty(incoming.getChatPrefix()).trim();
        sanitized.setChatPrefix(prefix.isEmpty() ? defaults.getChatPrefix() : prefix);
        return sanitized;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
