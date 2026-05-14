package br.com.reinodoce.mctiktok.tiktok;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TikTokLanguageResolverTest {
    private static final String EN_US_TAG = "en-US";

    @Test
    void resolvesMinecraftLocaleToTikTokLanguageTag() {
        assertEquals("pt-BR", TikTokLanguageResolver.resolve("pt_br"));
        assertEquals(EN_US_TAG, TikTokLanguageResolver.resolve("en_us"));
        assertEquals("fil-PH", TikTokLanguageResolver.resolve("fil_ph"));
    }

    @Test
    void preservesLanguageTagsAndFallsBackWhenBlank() {
        assertEquals("ja-JP", TikTokLanguageResolver.resolve("ja-JP"));
        assertEquals("es-419", TikTokLanguageResolver.resolve("es_419"));
        assertEquals(EN_US_TAG, TikTokLanguageResolver.resolve(""));
        assertEquals(EN_US_TAG, TikTokLanguageResolver.resolve(null));
        assertEquals(EN_US_TAG, TikTokLanguageResolver.resolve("not a locale"));
    }
}
