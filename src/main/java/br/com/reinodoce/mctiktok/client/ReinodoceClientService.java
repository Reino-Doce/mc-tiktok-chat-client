package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.chat.LiveMessageFormatter;
import br.com.reinodoce.mctiktok.chat.MinecraftChatGateway;
import br.com.reinodoce.mctiktok.client.font.InlineMediaFontHooks;
import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.command.ReinodoceCommandService;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.core.ReinodoceCoreService;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Client service that composes core TikTok logic with Minecraft inline media rendering.
 */
public class ReinodoceClientService implements ReinodoceCommandService {
    private final ReinodoceCoreService coreService;
    private final InlineMediaCache inlineMediaCache;
    private final InlineMediaTokenRegistry inlineMediaTokenRegistry;

    /**
     * Creates a client service for the supplied Minecraft platform adapter.
     *
     * @param platformBridge Minecraft platform adapter
     */
    public ReinodoceClientService(MinecraftPlatformBridge platformBridge) {
        this.inlineMediaCache = new InlineMediaCache();
        this.inlineMediaTokenRegistry = new InlineMediaTokenRegistry(inlineMediaCache);
        InlineMediaFontHooks.installRegistry(inlineMediaTokenRegistry);
        this.coreService = new ReinodoceCoreService(
                new MinecraftChatGateway(
                        Objects.requireNonNull(platformBridge, "platformBridge"),
                        new LiveMessageFormatter(inlineMediaTokenRegistry),
                        inlineMediaCache
                ),
                new ReinodoceConfigRepository()
        );
    }

    /**
     * Initializes core service state.
     */
    public void initialize() {
        coreService.initialize();
    }

    @Override
    public CommandResult connect(String username) {
        return coreService.connect(username);
    }

    @Override
    public CommandResult disconnect() {
        return coreService.disconnect();
    }

    @Override
    public List<String> statusLines() {
        List<String> lines = new ArrayList<>(coreService.statusLines());
        lines.add("Inline media renderer: font-coremod");
        lines.add("Coremod loaded: " + InlineMediaFontHooks.coremodLoaded());
        lines.add("Token registry: " + inlineMediaTokenRegistry.size());
        InlineMediaCache.Snapshot mediaSnapshot = inlineMediaCache.snapshot();
        lines.add("Media cache: resident=" + mediaSnapshot.resident()
                + " ready=" + mediaSnapshot.ready()
                + " loading=" + mediaSnapshot.loading()
                + " error=" + mediaSnapshot.error());
        lines.add("Downloads: started=" + mediaSnapshot.downloadsStarted()
                + " success=" + mediaSnapshot.downloadsSucceeded()
                + " fail=" + mediaSnapshot.downloadsFailed()
                + " disk=" + mediaSnapshot.diskHits()
                + " diskReloads=" + mediaSnapshot.diskReloads()
                + " memory=" + mediaSnapshot.memoryHits());
        lines.add("Cache evictions: ttl=" + mediaSnapshot.ttlEvictions() + " capacity=" + mediaSnapshot.capacityEvictions());
        return lines;
    }

    @Override
    public CommandResult setReconnectSeconds(int seconds) {
        return coreService.setReconnectSeconds(seconds);
    }

    @Override
    public CommandResult setFollowerRule(boolean enabled) {
        return coreService.setFollowerRule(enabled);
    }

    @Override
    public CommandResult setMinMemberLevelRule(int level) {
        return coreService.setMinMemberLevelRule(level);
    }

    @Override
    public CommandResult setSyntheticGift(int value) {
        return coreService.setSyntheticGift(value);
    }

    @Override
    public CommandResult setSyntheticGiftComboMode(String mode) {
        return coreService.setSyntheticGiftComboMode(mode);
    }

    @Override
    public CommandResult setSyntheticFollow(boolean enabled) {
        return coreService.setSyntheticFollow(enabled);
    }

    @Override
    public CommandResult setSyntheticJoin(boolean enabled) {
        return coreService.setSyntheticJoin(enabled);
    }

    @Override
    public CommandResult setSyntheticMemberLevel(boolean enabled) {
        return coreService.setSyntheticMemberLevel(enabled);
    }

    @Override
    public CommandResult setChatEmotesEnabled(boolean enabled) {
        return coreService.setChatEmotesEnabled(enabled);
    }

    @Override
    public CommandResult setChatLogEnabled(boolean enabled) {
        return coreService.setChatLogEnabled(enabled);
    }

    @Override
    public CommandResult setChatPrefix(String prefix) {
        return coreService.setChatPrefix(prefix);
    }

    @Override
    public CommandResult setChatFormat(String format) {
        return coreService.setChatFormat(format);
    }

    @Override
    public CommandResult reload() {
        return coreService.reload();
    }
}
