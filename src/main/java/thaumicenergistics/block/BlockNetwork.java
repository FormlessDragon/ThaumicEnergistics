package thaumicenergistics.block;

import ae2.api.implementations.IPowerChannelState;
import ae2.me.helpers.IGridConnectedTile;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * @author BrockWS
 * @author Alex811
 */
@Deprecated
public abstract class BlockNetwork extends BlockBase implements ITileEntityProvider {
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockNetwork(String id) {
        this(id, Material.IRON);
    }

    public BlockNetwork(String id, Material material) {
        super(id, material);
        this.setDefaultState(this.getBlockState().getBaseState()
                .withProperty(POWERED, false)
                .withProperty(ACTIVE, false)
        );
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote)
            return;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof IGridConnectedTile && placer instanceof EntityPlayer) {
            IGridConnectedTile tile = (IGridConnectedTile) te;
            tile.setOwner((EntityPlayer) placer);
            tile.getActionableNode();
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERED, ACTIVE);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        boolean powered = false;
        boolean active = false;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof IPowerChannelState) {
            IPowerChannelState powerState = (IPowerChannelState) te;
            powered = powerState.isPowered();
            active = powerState.isActive();
        }
        return super.getActualState(state, worldIn, pos)
                .withProperty(POWERED, powered)
                .withProperty(ACTIVE, active);
    }
}
