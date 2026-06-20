package thaumicenergistics.util;

import ae2.api.config.TerminalStyle;
import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumicenergistics.core.ThEFeatures;

import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @author BrockWS
 */
public class ThEUtil {

    @SuppressWarnings("unchecked")
    public static <T extends Enum<?>> T rotateEnum(T e, Set<? extends T> options, boolean backwards) {
        if (e == null || options == null)
            return e;
        Object[] optArr = options.toArray();
        int mappedOrdinal = IntStream.range(0, options.size())  // find e's index in options
                .parallel()
                .filter(i -> optArr[i] == e)
                .findFirst()
                .orElseThrow(ArrayIndexOutOfBoundsException::new);
        T next = (T) optArr[(mappedOrdinal + (backwards ? -1 : 1) + options.size()) % options.size()];
        return ThEUtil.isInvalidSetting(next) ? ThEUtil.rotateEnum(next, options, backwards) : next;
    }

    public static boolean isInvalidSetting(Enum e) {
        return e == TerminalStyle.FULL;
    }

    public static int getEssentiaCapacity(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof IEssentiaContainerItem) || stack.getItem().getRegistryName() == null)
            return 0;

        Map<String, Integer> capacities = ThEFeatures.instance().config().essentiaContainerCapacity();
        String registryName = stack.getItem().getRegistryName().toString();
        Integer capacity = capacities.get(registryName + ":" + stack.getMetadata());
        return capacity != null ? capacity : capacities.getOrDefault(registryName, 0);
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
