package thaumicenergistics.integration.hwyla;

import mobius.waila.api.IWailaConfigHandler;
import mobius.waila.api.IWailaDataAccessor;
import mobius.waila.api.IWailaDataProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import thaumicenergistics.api.IThELangKey;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.tile.ThENetworkPowerState;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Alex811
 */
public class TileWailaDataProvider implements IWailaDataProvider {
    @Nonnull
    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        TileEntity te = accessor.getTileEntity();
        if (te instanceof ThENetworkPowerState) {
            ((ThENetworkPowerState) te).withPowerStateText(tooltip::add, IThELangKey::getLocalizedKey);
            if (te instanceof TileArcaneAssembler)
                ((TileArcaneAssembler) te).withInfoText(tooltip::add, IThELangKey::getLocalizedKey);
        }
        return tooltip;
    }
}
