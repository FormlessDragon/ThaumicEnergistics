package thaumicenergistics.util;

import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumicenergistics.core.ThEConfig;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

/**
 * @author BrockWS
 */
public class ThEUtil {

    public static int getEssentiaCapacity(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof IEssentiaContainerItem) || stack.getItem().getRegistryName() == null)
            return 0;

        String registryName = stack.getItem().getRegistryName().toString();
        String keyedName = registryName + ":" + stack.getMetadata();
        Object2IntMap<String> capacities = ThEConfig.instance().essentiaContainerCapacity();
        if (capacities.containsKey(keyedName)) {
            return capacities.getInt(keyedName);
        }
        return capacities.getInt(registryName);
    }

    /**
     * Like {@link ForgeUtil#areItemStacksEqual(ItemStack, ItemStack)}, but safely compares items that were cheated in, that normally would have NBT.
     *
     * @param a 1st stack
     * @param b 2nd stack
     * @return true if they're equal
     */
    public static boolean areItemStacksEqual(ItemStack a, ItemStack b) {
        return a != null && b != null && ItemStack.areItemsEqual(a, b) && (a.hasTagCompound() == b.hasTagCompound()) && ForgeUtil.areNBTTagsEqual(a.getTagCompound(), b.getTagCompound());
    }

}
