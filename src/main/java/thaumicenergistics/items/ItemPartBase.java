package thaumicenergistics.items;

import ae2.api.parts.IPart;
import ae2.api.parts.IPartItem;
import ae2.api.parts.PartHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumicenergistics.client.render.IThEModel;

import javax.annotation.Nullable;

/**
 * @author BrockWS
 */
public abstract class ItemPartBase extends ItemBase implements IPartItem<IPart>, IThEModel {

    public ItemPartBase(String id) {
        super(id);
    }

    @Override
    public Class<IPart> getPartClass() {
        return IPart.class;
    }

    @Override
    public IPart createPart() {
        return this.createPartFromItemStack(ItemStack.EMPTY);
    }

    @Nullable
    public abstract IPart createPartFromItemStack(ItemStack stack);

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        return PartHelper.usePartItem(player.getHeldItem(hand), player, world, pos, hand, side, hitX, hitY, hitZ);
    }
}
