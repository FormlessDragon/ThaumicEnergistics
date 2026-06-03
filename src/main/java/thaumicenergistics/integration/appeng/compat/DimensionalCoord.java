package thaumicenergistics.integration.appeng.compat;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Local Stage 1 replacement for the old AE2 DimensionalCoord helper.
 */
public final class DimensionalCoord {
    private final World world;
    private final BlockPos pos;

    public DimensionalCoord(TileEntity tile) {
        this(tile.getWorld(), tile.getPos());
    }

    public DimensionalCoord(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    public DimensionalCoord(World world, int x, int y, int z) {
        this(world, new BlockPos(x, y, z));
    }

    public World getWorld() {
        return this.world;
    }

    public World getLevel() {
        return this.world;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
