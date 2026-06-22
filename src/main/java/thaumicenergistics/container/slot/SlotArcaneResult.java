package thaumicenergistics.container.slot;

import ae2.container.slot.OutputSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import thaumicenergistics.container.ICraftingContainer;

import java.util.Objects;

/**
 * @author BrockWS
 */
public class SlotArcaneResult extends OutputSlot {

    public SlotArcaneResult(ICraftingContainer container, EntityPlayer player, int index, int xPosition, int yPosition) {
        super(Objects.requireNonNull(Objects.requireNonNull(container, "container").getCraftingResultInventory(),
                "result inventory"), index, xPosition, yPosition);
        Objects.requireNonNull(player, "player");
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        return ItemStack.EMPTY;
    }
}
