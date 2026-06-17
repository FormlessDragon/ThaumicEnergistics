package thaumicenergistics.util.inventory;

import ae2.api.upgrades.Upgrades;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import thaumicenergistics.items.ItemKnowledgeCore;

import java.util.HashMap;
import java.util.Map;

/**
 * @author BrockWS
 */
public class ThEUpgradeInventory extends ThEInternalInventory {

    private boolean cached = false;
    private final Map<Item, Integer> cachedUpgrades;
    private ItemStack upgradable;

    public ThEUpgradeInventory(String customName, int size, int stackLimit, ItemStack upgradable) {
        this(customName, size, stackLimit);
        this.upgradable = upgradable;
    }

    public ThEUpgradeInventory(String customName, int size, int stackLimit) {
        super(customName, size, stackLimit);
        this.cachedUpgrades = new HashMap<>();
    }

    public boolean isKnowledgeCoreSlot() {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (stack.getItem() instanceof ItemKnowledgeCore && !isKnowledgeCoreSlot())
            return false;
        if (this.upgradable == null) // If the item/block/part that this is attached to is null, then just allow without checking max allowed
            return Upgrades.isUpgradeCardItem(stack);
        return this.getMaxUpgrades(stack) > 0 && this.getUpgrades(stack) < this.getMaxUpgrades(stack);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        this.cached = false;
    }

    public int getUpgrades(Object o) {
        if (!this.cached)
            this.calculateUpgrades();

        if (o instanceof ItemStack) {
            ItemStack stack = (ItemStack) o;
            o = stack.getItem();
        }

        return o instanceof Item ? this.cachedUpgrades.getOrDefault(o, 0) : 0;
    }

    private void calculateUpgrades() {
        this.cachedUpgrades.clear();
        this.iterator().forEachRemaining(stack -> {
            if (!stack.isEmpty()) {
                Item upgrade = stack.getItem();
                this.cachedUpgrades.put(upgrade, this.cachedUpgrades.getOrDefault(upgrade, 0) + stack.getCount());
            }
        });
        this.cached = true;
    }

    private int getMaxUpgrades(ItemStack upgradeStack) {
        if (upgradeStack.isEmpty() || this.upgradable == null || this.upgradable.isEmpty()) {
            return 0;
        }
        return Upgrades.getMaxInstallable(upgradeStack.getItem(), this.upgradable.getItem());
    }
}
