package thaumicenergistics.block;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.block.AEBaseTileBlock;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import thaumicenergistics.common.me.key.AEEssentiaKey;
import thaumicenergistics.tile.TileInfusionProvider;

/**
 * @author BrockWS
 */
public class BlockInfusionProvider extends AEBaseTileBlock<TileInfusionProvider> {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockInfusionProvider() {
        super(Material.IRON);
        this.setTileEntity(TileInfusionProvider.class);
        this.setDefaultState(this.blockState.getBaseState()
            .withProperty(ACTIVE, Boolean.FALSE)
            .withProperty(POWERED, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return createBlockState(ACTIVE, POWERED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(ACTIVE) ? 1 : 0) | (state.getValue(POWERED) ? 2 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
            .withProperty(ACTIVE, (meta & 1) != 0)
            .withProperty(POWERED, (meta & 2) != 0);
    }

    @Override
    protected IBlockState updateBlockStateFromTileEntity(IBlockState currentState, TileInfusionProvider tileEntity) {
        World world = tileEntity.getWorld();
        if (world != null && world.isRemote) {
            return currentState;
        }

        return currentState
            .withProperty(ACTIVE, tileEntity.getMainNode().isActive())
            .withProperty(POWERED, tileEntity.getMainNode().isPowered());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ)) {
            return true;
        }

        if (hand != EnumHand.MAIN_HAND) {
            return false;
        }

        TileInfusionProvider tile = this.getTileEntity(world, pos);
        if (tile != null) {
            if (world.isRemote) {
                return true;
            }

            if (player.isSneaking()) {
                KeyCounter storedAspects = tile.getStoredAspects();
                if (!storedAspects.isEmpty()) {
                    player.sendMessage(new TextComponentString("Stored Aspects:"));
                    for (Object2LongMap.Entry<AEKey> stack : storedAspects) {
                        if (stack.getKey() instanceof AEEssentiaKey key) {
                            player.sendMessage(new TextComponentString(key.getAspect().getName() + " = " + stack.getLongValue()));
                        }
                    }
                } else {
                    player.sendMessage(new TextComponentString("No aspects found"));
                }
            }
            tile.refreshVisualState();
            return true;
        }
        return false;
    }

}
