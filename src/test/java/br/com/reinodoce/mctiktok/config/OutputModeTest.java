package br.com.reinodoce.mctiktok.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputModeTest {
    @Test
    void parsesKnownOutputModesAndFallsBackToChat() {
        assertEquals(OutputMode.CHAT, OutputMode.fromString("chat"));
        assertEquals(OutputMode.ACTIONBAR, OutputMode.fromString("ACTIONBAR"));
        assertEquals(OutputMode.HUD, OutputMode.fromString("hud"));
        assertEquals(OutputMode.OFF, OutputMode.fromString("off"));
        assertEquals(OutputMode.CHAT, OutputMode.fromString("unknown"));
    }

    @Test
    void parsesKnownHudPositionsAndFallsBackToTopLeft() {
        assertEquals(HudPosition.TOP_LEFT, HudPosition.fromString("top-left"));
        assertEquals(HudPosition.TOP_RIGHT, HudPosition.fromString("TOP-RIGHT"));
        assertEquals(HudPosition.BOTTOM_LEFT, HudPosition.fromString("bottom-left"));
        assertEquals(HudPosition.BOTTOM_RIGHT, HudPosition.fromString("bottom-right"));
        assertEquals(HudPosition.TOP_LEFT, HudPosition.fromString("unknown"));
    }
}
