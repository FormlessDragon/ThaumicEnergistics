package thaumicenergistics.integration.hwyla;

import mobius.waila.api.IWailaRegistrar;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import thaumicenergistics.integration.IThEIntegration;
import thaumicenergistics.tile.ThENetworkTile;

/**
 * @author Alex811
 */
public class ThEHwyla implements IThEIntegration {
    @Override
    public void init() {
        FMLInterModComms.sendMessage(this.getModID(), "register", this.getClass().getName() + ".register");
    }

    public static void register(IWailaRegistrar registrar) {
        registrar.registerBodyProvider(new TileWailaDataProvider(), ThENetworkTile.class);
    }
}
