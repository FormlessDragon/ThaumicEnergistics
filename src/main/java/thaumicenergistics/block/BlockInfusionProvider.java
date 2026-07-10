package thaumicenergistics.block;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
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
public class BlockInfusionProvider extends BlockBase<TileInfusionProvider> {

    public BlockInfusionProvider() {
        super(TileInfusionProvider.class);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote || hand != EnumHand.MAIN_HAND)
            return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileInfusionProvider inf) {
            if (player.isSneaking()) {
                KeyCounter storedAspects = inf.getStoredAspects();
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
            inf.refreshVisualState();
            return true;
        }
        return false;
    }

}
