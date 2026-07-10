package thaumicenergistics.container.slot;

import ae2.api.inventories.InternalInventory;
import ae2.container.slot.AppEngSlot;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Alex811
 */
public class SlotKnowledgeCore extends AppEngSlot {
    private static final String TEXTURE = "thaumicenergistics:gui/slot/knowledge_core";

    public SlotKnowledgeCore(InternalInventory inventory, int index, int xPosition, int yPosition) {
        super(Objects.requireNonNull(inventory, "inventory"), index, xPosition, yPosition);
    }

    @Nullable
    @Override
    public String getSlotTexture() {
        return TEXTURE;
    }
}
