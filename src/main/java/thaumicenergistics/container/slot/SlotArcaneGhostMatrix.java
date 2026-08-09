package thaumicenergistics.container.slot;

import ae2.api.stacks.GenericStack;
import ae2.container.slot.FakeSlot;
import net.minecraft.item.ItemStack;
import thaumicenergistics.container.ICraftingContainer;
import thaumicenergistics.part.inventory.ArcaneInscriberMatrixInventory;

import java.util.Objects;

/**
 * @author Alex811
 */
public class SlotArcaneGhostMatrix extends FakeSlot {
    private final ICraftingContainer container;

    public SlotArcaneGhostMatrix(ICraftingContainer container, int index, int xPosition, int yPosition) {
        super(Objects.requireNonNull(Objects.requireNonNull(container, "container").getCraftingInventory(),
                "crafting inventory"), index, xPosition, yPosition);
        this.container = container;
    }

    @Override
    public boolean canSetFilterTo(ItemStack stack) {
        return this.getSlotIndex() < ArcaneInscriberMatrixInventory.INGREDIENT_SLOT_COUNT
            && !GenericStack.isWrapped(stack)
            && super.canSetFilterTo(stack);
    }

    @Override
    public void onSlotChanged() {
        this.container.onMatrixChanged();
        super.onSlotChanged();
    }
}
