package thaumicenergistics.container.slot;

import ae2.api.inventories.InternalInventory;
import ae2.api.inventories.PlatformInventoryWrapper;
import ae2.container.slot.AppEngSlot;
import net.minecraftforge.items.IItemHandler;

import java.util.Objects;

/**
 * Supergiant AE2 slot backed by an existing Forge item handler.
 *
 * <p>The migrated Thaumic Energistics slots can receive typed AE2 inventories directly. Legacy container call sites
 * still passing {@link IItemHandler} are bridged only through Supergiant's {@link PlatformInventoryWrapper}.</p>
 */
public class ThEAppEngSlot extends AppEngSlot {

    private final int y;
    private final boolean affectedBySlotCount;

    public ThEAppEngSlot(IItemHandler handler, int index, int xPosition, int yPosition) {
        this(handler, index, xPosition, yPosition, true);
    }

    public ThEAppEngSlot(IItemHandler handler, int index, int xPosition, int yPosition, boolean affectedBySlotCount) {
        this(new PlatformInventoryWrapper(Objects.requireNonNull(handler, "handler")), index, xPosition, yPosition,
                affectedBySlotCount);
    }

    public ThEAppEngSlot(InternalInventory inventory, int index, int xPosition, int yPosition) {
        this(inventory, index, xPosition, yPosition, true);
    }

    public ThEAppEngSlot(InternalInventory inventory, int index, int xPosition, int yPosition,
                         boolean affectedBySlotCount) {
        super(Objects.requireNonNull(inventory, "inventory"), index, xPosition, yPosition);
        this.y = yPosition;
        this.affectedBySlotCount = affectedBySlotCount;
    }

    public void recalculateY(int slots) {
        this.yPos = this.y;
        if (this.affectedBySlotCount) {
            this.yPos += slots * 18;
        }
    }
}
