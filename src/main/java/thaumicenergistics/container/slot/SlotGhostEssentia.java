package thaumicenergistics.container.slot;

import ae2.api.inventories.BaseInternalInventory;
import ae2.container.slot.FakeSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.util.EssentiaFilter;

import java.util.Objects;

/**
 * @author BrockWS
 */
public class SlotGhostEssentia extends FakeSlot {

    private final EssentiaFilter filter;

    public SlotGhostEssentia(EssentiaFilter filter, IInventory inventory, int index, int xPosition, int yPosition, int groupID) {
        super(new VanillaInventoryAdapter(Objects.requireNonNull(inventory, "inventory")), index, xPosition, yPosition);
        this.filter = Objects.requireNonNull(filter, "filter");
    }

    public EssentiaFilter getFilter() {
        return this.filter;
    }

    public Aspect getAspect() {
        return this.getFilter().getAspect(this.getSlotIndex());
    }

    public void setAspect(Aspect aspect) {
        getFilter().setAspect(aspect, this.getSlotIndex());
    }

    @Override
    @NotNull
    public ItemStack getStack() {
        AEEssentiaKey key = AEEssentiaKey.of(this.getAspect());
        if (key != null)
            return key.wrapForDisplayOrFilter();
        return ItemStack.EMPTY;
    }

    private static final class VanillaInventoryAdapter extends BaseInternalInventory {

        private final IInventory inventory;

        private VanillaInventoryAdapter(IInventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public int size() {
            return this.inventory.getSizeInventory();
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.inventory.getInventoryStackLimit();
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return this.inventory.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            this.inventory.setInventorySlotContents(slotIndex, stack);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return this.inventory.isItemValidForSlot(slot, stack);
        }
    }
}
