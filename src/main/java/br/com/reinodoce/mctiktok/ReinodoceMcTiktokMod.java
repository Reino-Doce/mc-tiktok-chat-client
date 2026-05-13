package br.com.reinodoce.mctiktok;

import br.com.reinodoce.mctiktok.client.ReinodoceClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge mod entry point for the client-side Reino Doce TikTok chat bridge.
 */
@Mod(ReinodoceMcTiktokMod.MOD_ID)
public class ReinodoceMcTiktokMod {
    /** Forge mod id used by resources and metadata. */
    public static final String MOD_ID = "reinodoce_mctiktok";

    /**
     * Registers client-only bootstrap work when Forge constructs the mod.
     */
    public ReinodoceMcTiktokMod() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ReinodoceClientBootstrap::initialize);
    }
}
