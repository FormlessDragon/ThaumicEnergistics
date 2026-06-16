package thaumicenergistics.integration.theoneprobe;

import mcjty.theoneprobe.api.*;
import ae2.api.parts.IPart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumicenergistics.thaumicenergistics.Reference;

/**
 * @author Alex811
 */
public class PartTOPDisplayOverride implements IBlockDisplayOverride {
    @Override
    public boolean overrideStandardInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        IPart part = TOPPartAccessor.getPart(world.getTileEntity(data.getPos()), data);
        if (part != null) {
            ItemStack partStack = part.getPartItem().asItemStack();
            probeInfo.horizontal()
                    .item(partStack)
                    .vertical()
                    .itemLabel(partStack)
                    .text(TextStyleClass.MODNAME + Reference.MOD_NAME);
            return true;
        }
        return false;
    }
}
