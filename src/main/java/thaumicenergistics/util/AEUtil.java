package thaumicenergistics.util;

import ae2.util.InteractionUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

/**
 * @author BrockWS
 */
public class AEUtil {

    public static String getModID(Object o) {
        ResourceLocation rl = null;
        if (o instanceof ItemStack)
            return AEUtil.getModID(((ItemStack) o).getItem());
        else if (o instanceof EntityItem)
            return AEUtil.getModID(((EntityItem) o).getItem());
        else if (o instanceof Item)
            rl = ((Item) o).getRegistryName();
        else if (o instanceof Block)
            rl = ((Block) o).getRegistryName();

        return rl != null ? rl.getNamespace() : "MODIDNOTFOUND";
    }

    public static boolean isWrench(ItemStack stack, EntityPlayer player, BlockPos pos) {
        if (stack.isEmpty())
            return false;
        return InteractionUtil.canWrenchRotate(player, stack, pos);
    }

}
