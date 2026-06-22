package thaumicenergistics.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author BrockWS
 * @author Alex811
 */
public class ItemHandlerUtil {

    @Nonnull
    public static ItemStack insert(IItemHandler handler, ItemStack stack) {
        return ItemHandlerUtil.insert(handler, stack, false);
    }

    @Nonnull
    public static ItemStack insert(IItemHandler handler, ItemStack stack, boolean simulate) {
        return ItemHandlerUtil.insert(handler, stack, simulate, 0, handler.getSlots());
    }

    @Nonnull
    public static ItemStack extract(IItemHandler handler, ItemStack stack) {
        return ItemHandlerUtil.extract(handler, stack, false);
    }

    @Nonnull
    public static ItemStack extract(IItemHandler handler, ItemStack stack, boolean simulate) {
        return ItemHandlerUtil.extract(handler, stack, simulate, 0, handler.getSlots());
    }

    @Nonnull
    public static ItemStack insert(IItemHandler handler, ItemStack original, boolean simulate, int minValidSlot, int maxValidSlot) {
        if (original == null || original.isEmpty())
            return ItemStack.EMPTY;

        ItemStack copy = original.copy();
        List<Integer> emptySlots = new ArrayList<>();

        for (int slot = minValidSlot; slot < maxValidSlot; slot++) {   // insert into matching stacks
            ItemStack existing = handler.getStackInSlot(slot);
            if (ThEUtil.areItemStacksEqual(existing, copy)) {
                copy = handler.insertItem(slot, copy, simulate);
                if (copy.isEmpty())
                    return ItemStack.EMPTY;
            } else if (existing.isEmpty() && handler.isItemValid(slot, copy))
                emptySlots.add(slot);
        }

        for (int slot : emptySlots) {   // insert the rest into empty slots
            copy = handler.insertItem(slot, copy, simulate);
            if (copy.isEmpty())
                return ItemStack.EMPTY;
        }

        return copy;    // leftover or empty stack
    }

    @Nonnull
    public static ItemStack extract(IItemHandler handler, ItemStack original, boolean simulate, int minValidSlot, int maxValidSlot) {
        if (original == null || original.isEmpty())
            return ItemStack.EMPTY;

        ItemStack extracted = null;

        for (int slot = minValidSlot; slot < maxValidSlot; slot++) {
            if (extracted != null && original.getCount() == extracted.getCount()) {
                return extracted;
            }
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty() || !ThEUtil.areItemStacksEqual(original, inSlot))
                continue;
            if (extracted == null) {
                extracted = handler.extractItem(slot, original.getCount(), simulate);
                continue;
            }
            ItemStack s = handler.extractItem(slot, original.getCount() - extracted.getCount(), simulate);
            if (s.isEmpty() || !ThEUtil.areItemStacksEqual(original, s))
                continue;
            extracted.grow(s.getCount());
        }
        return extracted == null || extracted.isEmpty() ? ItemStack.EMPTY : extracted;
    }

}
