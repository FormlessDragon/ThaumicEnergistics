package thaumicenergistics.container.slot;

import ae2.container.slot.FakeSlot;
import thaumicenergistics.container.ICraftingContainer;

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
    public void onSlotChanged() {
        this.container.onMatrixChanged();
        super.onSlotChanged();
    }
}
