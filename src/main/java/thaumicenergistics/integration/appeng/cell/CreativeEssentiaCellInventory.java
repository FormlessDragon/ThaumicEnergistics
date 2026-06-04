package thaumicenergistics.integration.appeng.cell;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.cells.CellState;
import ae2.api.storage.cells.StorageCell;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.items.ItemCreativeEssentiaCell;
import thaumcraft.api.aspects.Aspect;

import java.util.Set;

/**
 * @author BrockWS
 */
public class CreativeEssentiaCellInventory implements StorageCell {

    private final ItemStack stack;

    public CreativeEssentiaCellInventory(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        return amount > 0 && this.isConfiguredEssentiaKey(what) ? amount : 0;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        return amount > 0 && this.isConfiguredEssentiaKey(what) ? amount : 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        Set<AEKey> configuredKeys = this.getConfiguredKeys();
        if (configuredKeys.isEmpty()) {
            for (Aspect aspect : Aspect.aspects.values()) {
                AEEssentiaKey key = AEEssentiaKey.of(aspect);
                if (key != null) {
                    out.add(key, Long.MAX_VALUE);
                }
            }
        } else {
            for (AEKey key : configuredKeys) {
                if (key instanceof AEEssentiaKey) {
                    out.add(key, Long.MAX_VALUE);
                }
            }
        }
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return this.isConfiguredEssentiaKey(what);
    }

    @Override
    public CellState getStatus() {
        return CellState.TYPES_FULL;
    }

    @Override
    public double getIdleDrain() {
        return 0;
    }

    @Override
    public boolean canFitInsideCell() {
        return false;
    }

    @Override
    public ITextComponent getDescription() {
        return new TextComponentString(this.stack.getDisplayName());
    }

    @Override
    public void persist() {
    }

    private boolean isConfiguredEssentiaKey(AEKey key) {
        if (!(key instanceof AEEssentiaKey)) {
            return false;
        }

        Set<AEKey> configuredKeys = this.getConfiguredKeys();
        return configuredKeys.isEmpty() || configuredKeys.contains(key);
    }

    private Set<AEKey> getConfiguredKeys() {
        return ((ItemCreativeEssentiaCell) this.stack.getItem()).getConfigInventory(this.stack).keySet();
    }
}
