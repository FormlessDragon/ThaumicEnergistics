package thaumicenergistics.util.inventory;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.Upgrades;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import thaumicenergistics.items.ItemKnowledgeCore;

import java.util.HashMap;
import java.util.Map;

/**
 * @author BrockWS
 */
public class ThEUpgradeInventory extends ThEInternalInventory implements IUpgradeInventory {

    private boolean cached = false;
    private final Map<Item, Integer> cachedUpgrades;
    private ItemStack upgradable;

    public ThEUpgradeInventory(String customName, int size, int stackLimit, ItemStack upgradable) {
        this(customName, size, stackLimit);
        if (upgradable.isEmpty()) {
            throw new IllegalArgumentException("Upgradable item stack must not be empty");
        }
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
        if (stack.isEmpty())
            return false;
        if (stack.getItem() instanceof ItemKnowledgeCore && !isKnowledgeCoreSlot())
            return false;
        Item upgrade = stack.getItem();
        int maxInstalled = this.getMaxInstalled(upgrade);
        return maxInstalled > 0 && this.getInstalledUpgrades(upgrade) < maxInstalled;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        this.cached = false;
    }

    @Override
    public Item getUpgradableItem() {
        if (this.upgradable == null || this.upgradable.isEmpty()) {
            throw new IllegalStateException("Upgrade inventory has no upgradable item");
        }
        return this.upgradable.getItem();
    }

    @Override
    public int getInstalledUpgrades(Item upgrade) {
        if (!this.cached)
            this.calculateUpgrades();
        return this.cachedUpgrades.getOrDefault(upgrade, 0);
    }

    @Override
    public int getMaxInstalled(Item upgrade) {
        return Upgrades.getMaxInstallable(upgrade, this.getUpgradableItem());
    }

    @Override
    public void readFromNBT(NBTTagCompound data, String subtag) {
        if (!data.hasKey(subtag)) {
            return;
        }
        NBTBase tag = data.getTag(subtag);
        if (!(tag instanceof NBTTagList)) {
            throw new IllegalArgumentException("Inventory subtag '" + subtag + "' must be a list tag");
        }
        this.deserializeNBT((NBTTagList) tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound data, String subtag) {
        data.setTag(subtag, this.serializeNBT(true));
    }

    @Deprecated
    public int getUpgrades(Object o) {
        if (o instanceof ItemStack) {
            ItemStack stack = (ItemStack) o;
            o = stack.getItem();
        }

        return o instanceof Item ? this.getInstalledUpgrades((Item) o) : 0;
    }

    private void calculateUpgrades() {
        this.cachedUpgrades.clear();
        this.iterator().forEachRemaining(stack -> {
            if (!stack.isEmpty()) {
                Item upgrade = stack.getItem();
                int maxInstalled = this.getMaxInstalled(upgrade);
                if (maxInstalled > 0) {
                    int installed = this.cachedUpgrades.getOrDefault(upgrade, 0) + stack.getCount();
                    this.cachedUpgrades.put(upgrade, Math.min(maxInstalled, installed));
                }
            }
        });
        this.cached = true;
    }
}
