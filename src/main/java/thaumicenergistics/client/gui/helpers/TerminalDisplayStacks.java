package thaumicenergistics.client.gui.helpers;

import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import net.minecraft.item.ItemStack;
import thaumicenergistics.me.key.AEEssentiaKey;

public final class TerminalDisplayStacks {
    private TerminalDisplayStacks() {
    }

    public static TerminalDisplayStack of(AEKey key, long amount, boolean craftable) {
        return key == null ? null : new TerminalDisplayStack(key, amount, craftable);
    }

    public static TerminalDisplayStack item(ItemStack stack, long amount, boolean craftable) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return of(AEItemKey.of(stack), amount, craftable);
    }

    public static TerminalDisplayStack essentia(AEEssentiaKey key, long amount, boolean craftable) {
        return of(key, amount, craftable);
    }
}
