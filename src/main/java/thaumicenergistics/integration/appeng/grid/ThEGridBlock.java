package thaumicenergistics.integration.appeng.grid;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumicenergistics.integration.appeng.compat.DimensionalCoord;

import javax.annotation.Nonnull;

/**
 * @author BrockWS
 */
public class ThEGridBlock {

    private final IThEGridHost host;
    private ItemStack rep;
    private final TileEntity repTE;
    private final boolean inWorld;
    private double idlePower = 1;

    public ThEGridBlock(IThEGridHost host, TileEntity rep, boolean inWorld) {
        this.host = host;
        this.repTE = rep;
        this.inWorld = inWorld;
    }

    public ThEGridBlock(IThEGridHost host, ItemStack rep, boolean inWorld) {
        this.host = host;
        this.rep = rep;
        this.repTE = null;
        this.inWorld = inWorld;
    }

    public double getIdlePowerUsage() {
        return this.idlePower;
    }

    public boolean isWorldAccessible() {
        return this.inWorld;
    }

    @Nonnull
    public DimensionalCoord getLocation() {
        return this.host.getLocation();
    }

    public void gridChanged() {
        this.host.gridChanged();
    }

    @Nonnull
    public ItemStack getMachineRepresentation() {
        if (this.repTE != null) {
            World world = this.repTE.getWorld();
            BlockPos pos = this.repTE.getPos();
            IBlockState state = world.getBlockState(pos);
            return new ItemStack(state.getBlock());
        }
        if (this.rep == null)
            return ItemStack.EMPTY;
        return this.rep;
    }
}
