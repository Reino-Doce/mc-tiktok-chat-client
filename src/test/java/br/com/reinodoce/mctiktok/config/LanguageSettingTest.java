package br.com.reinodoce.mctiktok.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageSettingTest {
    @Test
    void parseAcceptsAutoAndNormalizesLocales() {
        assertEquals("auto", LanguageSetting.parse(" AUTO ").orElseThrow());
        assertEquals("pt_br", LanguageSetting.parse("pt-BR").orElseThrow());
        assertEquals("fil_ph", LanguageSetting.parse("FIL_ph").orElseThrow());
    }

    @Test
    void parseRejectsInvalidLocaleValues() {
        assertTrue(LanguageSetting.parse("").isEmpty());
        assertTrue(LanguageSetting.parse("not").isEmpty());
        assertTrue(LanguageSetting.parse("not a locale").isEmpty());
        assertTrue(LanguageSetting.parse("pt-br-extra").isEmpty());
    }

    @Test
    void effectiveLanguageUsesOverrideOrMinecraftLocale() {
        assertEquals("de_de", LanguageSetting.resolveEffective("de_de", "pt_br"));
        assertEquals("pt_br", LanguageSetting.resolveEffective("auto", "pt-BR"));
        assertEquals("en_us", LanguageSetting.resolveEffective("auto", "not a locale"));
    }
}
