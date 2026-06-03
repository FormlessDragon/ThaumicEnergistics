package thaumicenergistics.container.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import thaumicenergistics.client.gui.helpers.MERepo;
import thaumicenergistics.client.gui.helpers.TerminalDisplayStack;

/**
 * @author BrockWS
 */
public class SlotME extends ThESlot {

    private static IInventory EMPTY = new InventoryBasic("[Null]", true, 0);

    private MERepo repo;

    public SlotME(MERepo repo, int index, int xPosition, int yPosition) {
        super(null, index, xPosition, yPosition, false);
        this.repo = repo;
    }

    public TerminalDisplayStack getAEStack() {
        return this.repo.getReferenceStack(this.getSlotIndex());
    }

    public TerminalDisplayStack getDisplayStack() {
        return this.getAEStack();
    }

    @Override
    public ItemStack getStack() {
        TerminalDisplayStack stack = this.getDisplayStack();
        if (stack == null || stack.asItemStackRepresentation() == null)
            return ItemStack.EMPTY;
        return stack.asItemStackRepresentation();
    }

    @Override
    public boolean getHasStack() {
        return !this.getStack().isEmpty();
    }

    @Override
    public void putStack(ItemStack stack) {

    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }
}
