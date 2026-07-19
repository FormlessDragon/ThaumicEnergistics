package thaumicenergistics.container.slot;

import ae2.container.slot.AppEngSlot;
import thaumicenergistics.container.ICraftingContainer;

import java.util.Objects;

/**
 * @author BrockWS
 */
public class SlotArcaneMatrix extends AppEngSlot {

    private final ICraftingContainer container;

    public SlotArcaneMatrix(ICraftingContainer container, int index, int xPosition, int yPosition) {
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
