package thaumicenergistics.block;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import thaumicenergistics.api.stacks.AEEssentiaKey;
import thaumicenergistics.client.render.IThEModel;
import thaumicenergistics.tile.TileInfusionProvider;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author BrockWS
 */
public class BlockInfusionProvider extends BlockNetwork implements IThEModel {

    public BlockInfusionProvider(String id) {
        super(id);
    }

    @Override
    public void registerTileEntity() {
        super.registerTileEntity();
        GameRegistry.registerTileEntity(TileInfusionProvider.class, Objects.requireNonNull(this.getRegistryName()));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote || hand != EnumHand.MAIN_HAND)
            return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileInfusionProvider) {
            TileInfusionProvider inf = (TileInfusionProvider) te;
            if (player.isSneaking()) {
                KeyCounter storedAspects = inf.getStoredAspects();
                if (!storedAspects.isEmpty()) {
                    player.sendMessage(new TextComponentString("Stored Aspects:"));
                    for (Object2LongMap.Entry<AEKey> stack : storedAspects) {
                        if (stack.getKey() instanceof AEEssentiaKey) {
                            AEEssentiaKey key = (AEEssentiaKey) stack.getKey();
                            player.sendMessage(new TextComponentString(key.getAspect().getName() + " = " + stack.getLongValue()));
                        }
                    }
                } else {
                    player.sendMessage(new TextComponentString("No aspects found"));
                }
            }
            // FIXME: Make sure it updates itself, can be removed if TileInfusionProvider monitors the me network for changes
            te.markDirty();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileInfusionProvider();
    }

    @Override
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(Objects.requireNonNull(this.getRegistryName()), "inventory"));
    }
}
