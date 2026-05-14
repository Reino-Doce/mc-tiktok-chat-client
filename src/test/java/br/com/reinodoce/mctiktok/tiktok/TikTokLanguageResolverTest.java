package br.com.reinodoce.mctiktok.tiktok;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TikTokLanguageResolverTest {
    @Test
    void resolvesMinecraftLocaleToTikTokLanguageTag() {
        assertEquals("pt-BR", TikTokLanguageResolver.resolve("pt_br"));
        assertEquals("en-US", TikTokLanguageResolver.resolve("en_us"));
        assertEquals("fil-PH", TikTokLanguageResolver.resolve("fil_ph"));
    }

    @Test
    void preservesLanguageTagsAndFallsBackWhenBlank() {
        assertEquals("ja-JP", TikTokLanguageResolver.resolve("ja-JP"));
        assertEquals("en-US", TikTokLanguageResolver.resolve(""));
        assertEquals("en-US", TikTokLanguageResolver.resolve(null));
    }
}
