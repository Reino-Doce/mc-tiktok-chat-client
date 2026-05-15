package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.client.font.InlineMediaFontHooks;
import br.com.reinodoce.mctiktok.client.font.InlineMediaTokenRegistry;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats client-side inline media diagnostics for commands and support reports.
 */
final class InlineMediaDiagnostics {
    private static final String RENDERER = "font-coremod";

    private InlineMediaDiagnostics() {
    }

    static List<String> statusLines(InlineMediaTokenRegistry tokenRegistry, InlineMediaCache mediaCache) {
        InlineMediaCache.Snapshot snapshot = mediaCache.snapshot();
        List<String> lines = new ArrayList<>();
        lines.add("Inline media renderer: " + RENDERER);
        lines.add("Coremod loaded: " + coremodLoaded());
        lines.add("Token registry: " + tokenRegistry.size());
        lines.add("Media cache: resident=" + snapshot.resident()
                + " ready=" + snapshot.ready()
                + " loading=" + snapshot.loading()
                + " error=" + snapshot.error());
        lines.add("Downloads: started=" + snapshot.downloadsStarted()
                + " success=" + snapshot.downloadsSucceeded()
                + " fail=" + snapshot.downloadsFailed()
                + " disk=" + snapshot.diskHits()
                + " diskReloads=" + snapshot.diskReloads()
                + " memory=" + snapshot.memoryHits());
        lines.add("Cache evictions: ttl=" + snapshot.ttlEvictions() + " capacity=" + snapshot.capacityEvictions());
        return lines;
    }

    static Map<String, Object> report(InlineMediaTokenRegistry tokenRegistry, InlineMediaCache mediaCache) {
        InlineMediaCache.Snapshot snapshot = mediaCache.snapshot();
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("resident", snapshot.resident());
        cache.put("ready", snapshot.ready());
        cache.put("loading", snapshot.loading());
        cache.put("error", snapshot.error());
        cache.put("downloadsStarted", snapshot.downloadsStarted());
        cache.put("downloadsSucceeded", snapshot.downloadsSucceeded());
        cache.put("downloadsFailed", snapshot.downloadsFailed());
        cache.put("diskHits", snapshot.diskHits());
        cache.put("diskReloads", snapshot.diskReloads());
        cache.put("memoryHits", snapshot.memoryHits());
        cache.put("ttlEvictions", snapshot.ttlEvictions());
        cache.put("capacityEvictions", snapshot.capacityEvictions());

        Map<String, Object> client = new LinkedHashMap<>();
        client.put("inlineMediaRenderer", RENDERER);
        client.put("coremodLoaded", coremodLoaded());
        client.put("tokenRegistrySize", tokenRegistry.size());
        client.put("inlineMediaCache", cache);
        return client;
    }

    private static boolean coremodLoaded() {
        try {
            return InlineMediaFontHooks.coremodLoaded();
        } catch (LinkageError exception) {
            return false;
        }
    }
}
