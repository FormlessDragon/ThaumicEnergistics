package thaumicenergistics.part.inventory;

import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * AE2-native matrix inventory for the Arcane Inscriber.
 * <p>
 * Slots 0-8 are recipe ghosts and therefore always store a single item. Slots 9-14 encode the real crystal amount
 * required by an arcane recipe and intentionally retain their original stack counts.
 */
public final class ArcaneInscriberMatrixInventory extends AppEngInternalInventory {

    public static final int INGREDIENT_SLOT_COUNT = 9;
    public static final int TOTAL_SLOT_COUNT = 15;

    public ArcaneInscriberMatrixInventory(InternalInventoryHost host) {
        super(host, TOTAL_SLOT_COUNT, 64);
        for (int slot = 0; slot < INGREDIENT_SLOT_COUNT; slot++) {
            this.setMaxStackSize(slot, 1);
        }
    }

    @Override
    public void setItemDirect(int slot, ItemStack stack) {
        super.setItemDirect(slot, this.normalize(slot, stack));
    }

    @Override
    public void readFromNBT(NBTTagCompound data, String name) {
        this.clear();
        super.readFromNBT(data, name);
        for (int slot = 0; slot < INGREDIENT_SLOT_COUNT; slot++) {
            ItemStack stack = super.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getCount() != 1) {
                super.setItemDirect(slot, this.normalize(slot, stack));
            }
        }
    }

    private ItemStack normalize(int slot, ItemStack stack) {
        if (slot >= INGREDIENT_SLOT_COUNT || stack.isEmpty() || stack.getCount() == 1) {
            return stack;
        }

        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        return normalized;
    }

}
