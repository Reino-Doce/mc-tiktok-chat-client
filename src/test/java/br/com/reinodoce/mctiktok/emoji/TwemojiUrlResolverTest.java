package br.com.reinodoce.mctiktok.emoji;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TwemojiUrlResolverTest {
    @Test
    void buildsIconIdWithoutVariationSelectors() {
        assertEquals("2764", TwemojiUrlResolver.toIconId(new int[]{0x2764, 0xFE0F}));
        assertEquals("1f1e7-1f1f7", TwemojiUrlResolver.toIconId(new int[]{0x1F1E7, 0x1F1F7}));
        assertEquals("1f469-200d-1f4bb", TwemojiUrlResolver.toIconId(new int[]{0x1F469, 0x200D, 0x1F4BB}));
    }

    @Test
    void buildsTwemojiUrl() {
        assertEquals(
                "https://cdn.jsdelivr.net/gh/jdecked/twemoji@17.0.2/assets/72x72/1f600.png",
                TwemojiUrlResolver.toUrl("1f600")
        );
    }
}
