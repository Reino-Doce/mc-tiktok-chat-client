package br.com.reinodoce.mctiktok.platform.mc1201;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Forge1201PlatformBridgeTest {
    @Test
    void alertSoundUsesVanillaToastUiSoundOnMasterSource() {
        SoundInstance sound = Forge1201PlatformBridge.createAlertSoundInstance();

        assertEquals(ResourceLocation.withDefaultNamespace("ui.toast.in"), sound.getLocation());
        assertEquals(SoundSource.MASTER, sound.getSource());
        assertEquals(SoundInstance.Attenuation.NONE, sound.getAttenuation());
        assertTrue(sound.isRelative());
    }
}
