package thaumicenergistics.client.gui.helpers;

import ae2.api.stacks.AEKey;
import net.minecraft.item.ItemStack;

public class TerminalDisplayStack {
    private final AEKey key;
    private final long stackSize;
    private final boolean craftable;

    public TerminalDisplayStack(AEKey key, long stackSize, boolean craftable) {
        this.key = key;
        this.stackSize = stackSize;
        this.craftable = craftable;
    }

    public AEKey key() {
        return this.key;
    }

    public ItemStack asItemStackRepresentation() {
        return this.key == null ? ItemStack.EMPTY : this.key.wrapForDisplayOrFilter();
    }

    public long stackSize() {
        return this.stackSize;
    }

    public boolean craftable() {
        return this.craftable;
    }

    public TerminalDisplayStack copy() {
        return new TerminalDisplayStack(this.key, this.stackSize, this.craftable);
    }
}
