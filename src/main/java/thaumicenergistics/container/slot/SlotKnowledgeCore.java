package thaumicenergistics.container.slot;

import ae2.api.inventories.InternalInventory;
import thaumicenergistics.core.ThEFeatures;

import javax.annotation.Nullable;

/**
 * @author Alex811
 */
public class SlotKnowledgeCore extends ThEAppEngSlot {
    public SlotKnowledgeCore(InternalInventory inventory, int index, int xPosition, int yPosition) {
        super(inventory, index, xPosition, yPosition);
    }

    @Nullable
    @Override
    public String getSlotTexture() {
        return ThEFeatures.instance().textures().knowledgeCoreSlot().toString();
    }
}
