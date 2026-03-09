package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.chat.LiveMessageFormatter;
import br.com.reinodoce.mctiktok.chat.MinecraftChatGateway;
import br.com.reinodoce.mctiktok.client.font.InlineMediaFontHooks;
import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.command.CommandResult;
import br.com.reinodoce.mctiktok.config.ReinodoceConfigRepository;
import br.com.reinodoce.mctiktok.core.ReinodoceCoreService;
import br.com.reinodoce.mctiktok.platform.MinecraftPlatformBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReinodoceClientService {
    private final ReinodoceCoreService coreService;
    private final InlineMediaCache inlineMediaCache;
    private final InlineMediaTokenRegistry inlineMediaTokenRegistry;

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

    public void initialize() {
        coreService.initialize();
    }

    public CommandResult connect(String username) {
        return coreService.connect(username);
    }

    public CommandResult disconnect() {
        return coreService.disconnect();
    }

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

    public CommandResult setReconnectSeconds(int seconds) {
        return coreService.setReconnectSeconds(seconds);
    }

    public CommandResult setFollowerRule(boolean enabled) {
        return coreService.setFollowerRule(enabled);
    }

    public CommandResult setMinMemberLevelRule(int level) {
        return coreService.setMinMemberLevelRule(level);
    }

    public CommandResult setSynteticGift(int value) {
        return coreService.setSynteticGift(value);
    }

    public CommandResult setSynteticGiftComboMode(String mode) {
        return coreService.setSynteticGiftComboMode(mode);
    }

    public CommandResult setSynteticFollow(boolean enabled) {
        return coreService.setSynteticFollow(enabled);
    }

    public CommandResult setSynteticJoin(boolean enabled) {
        return coreService.setSynteticJoin(enabled);
    }

    public CommandResult setSynteticMemberLevel(boolean enabled) {
        return coreService.setSynteticMemberLevel(enabled);
    }

    public CommandResult setChatEmotesEnabled(boolean enabled) {
        return coreService.setChatEmotesEnabled(enabled);
    }

    public CommandResult reload() {
        return coreService.reload();
    }
}
