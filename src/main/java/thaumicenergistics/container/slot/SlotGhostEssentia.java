package thaumicenergistics.container.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.util.EssentiaFilter;

import javax.annotation.Nonnull;

/**
 * @author BrockWS
 */
public class SlotGhostEssentia extends SlotGhost {

    private final EssentiaFilter filter;

    public SlotGhostEssentia(EssentiaFilter filter, IInventory inventory, int index, int xPosition, int yPosition, int groupID) {
        super(inventory, index, xPosition, yPosition, groupID);
        this.filter = filter;
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
    @Nonnull
    public ItemStack getStack() {
        AEEssentiaKey key = AEEssentiaKey.of(this.getAspect());
        if (key != null)
            return key.wrapForDisplayOrFilter();
        return ItemStack.EMPTY;
    }
}
