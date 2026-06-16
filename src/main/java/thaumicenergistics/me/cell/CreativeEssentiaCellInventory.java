package thaumicenergistics.me.cell;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.items.contents.CellConfig;
import ae2.me.cells.CreativeCellInventory;
import ae2.text.TextComponentItemStack;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.me.key.AEEssentiaKeys;

import java.util.Collections;

/**
 * @author BrockWS
 */
public class CreativeEssentiaCellInventory extends CreativeCellInventory {

    private final ObjectSet<AEKey> configured;
    private final ItemStack stack;

    public CreativeEssentiaCellInventory(ItemStack stack) {
        super(stack);
        this.stack = stack;
        this.configured = new ObjectOpenHashSet<>(CellConfig.create(Collections.singleton(AEEssentiaKeys.INSTANCE), stack).keySet());
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        return this.configured.contains(what) ? amount : 0;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        return this.configured.contains(what) ? amount : 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        if(this.configured.isEmpty()) {
            for(Aspect aspect : Aspect.aspects.values()) {
                AEEssentiaKey key = AEEssentiaKey.of(aspect);
                if (key != null) {
                    out.add(key, Long.MAX_VALUE);
                }
            }
        }else {
            for(AEKey key : this.configured) {
                if(key instanceof AEEssentiaKey) {
                    out.add(key, Long.MAX_VALUE);
                }
            }
        }
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return this.configured.contains(what);
    }

    @Override
    public boolean canFitInsideCell() {
        return false;
    }

    @Override
    public ITextComponent getDescription() {
        return TextComponentItemStack.of(this.stack);
    }
}
