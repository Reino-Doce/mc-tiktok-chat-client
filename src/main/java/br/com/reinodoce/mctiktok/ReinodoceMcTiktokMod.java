package br.com.reinodoce.mctiktok;

import br.com.reinodoce.mctiktok.client.ReinodoceClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(ReinodoceMcTiktokMod.MOD_ID)
public class ReinodoceMcTiktokMod {
    public static final String MOD_ID = "reinodoce_mctiktok";

    public ReinodoceMcTiktokMod() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ReinodoceClientBootstrap::initialize);
    }
}
