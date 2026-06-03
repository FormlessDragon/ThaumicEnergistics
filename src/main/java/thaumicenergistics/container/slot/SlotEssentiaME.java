package thaumicenergistics.container.slot;

import net.minecraft.item.ItemStack;
import thaumicenergistics.client.gui.helpers.EssentiaRepo;
import thaumicenergistics.client.gui.helpers.TerminalDisplayStack;

/**
 * Essentia terminal slot backed by TE's local essentia repo.
 */
public class SlotEssentiaME extends ThESlot {

    private final EssentiaRepo repo;

    public SlotEssentiaME(EssentiaRepo repo, int index, int xPosition, int yPosition) {
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
        ItemStack representation = stack == null ? null : stack.asItemStackRepresentation();
        if (representation == null) {
            return ItemStack.EMPTY;
        }
        return representation;
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
