package thaumicenergistics.api.ids;

import net.minecraft.util.ResourceLocation;
import thaumicenergistics.ThaumicEnergistics;

public final class ThEPartIds {

    public static final ResourceLocation ARCANE_TERMINAL = id("arcane_terminal");
    public static final ResourceLocation ARCANE_INSCRIBER = id("arcane_inscriber");
    public static final ResourceLocation ARCANE_P2P_TUNNEL = id("arcane_p2p_tunnel");

    private ThEPartIds() {
    }

    private static ResourceLocation id(String id) {
        return ThaumicEnergistics.id(id);
    }
}
